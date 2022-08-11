/*
 * Copyright 1999-2022 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.alibaba.nacos.istio.xds;

import com.alibaba.nacos.istio.api.ApiGenerator;
import com.alibaba.nacos.istio.common.ResourceSnapshot;
import com.alibaba.nacos.istio.misc.IstioConfig;
import com.alibaba.nacos.istio.model.IstioEndpoint;
import com.alibaba.nacos.istio.model.IstioService;
import com.google.protobuf.Any;
import com.google.protobuf.UInt32Value;
import io.envoyproxy.envoy.config.core.v3.TrafficDirection;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.nacos.istio.api.ApiConstants.ENDPOINT_TYPE;
import static com.alibaba.nacos.istio.util.IstioCrdUtil.buildClusterName;

/**
 * EdsGenerator.
 * @author RocketEngine26
 */
public final class EdsGenerator implements ApiGenerator<Any> {
   
    private static volatile EdsGenerator singleton = null;
    
    public static EdsGenerator getInstance() {
        if (singleton == null) {
            synchronized (ServiceEntryXdsGenerator.class) {
                if (singleton == null) {
                    singleton = new EdsGenerator();
                }
            }
        }
        return singleton;
    }
    
    @Override
    public List<Any> generate(ResourceSnapshot resourceSnapshot) {
        List<Any> result = new ArrayList<>();
        IstioConfig istioConfig = resourceSnapshot.getIstioConfig();
        Map<String, IstioService> istioServiceMap = resourceSnapshot.getIstioContext().getIstioServiceMap();
        
        for (Map.Entry<String, IstioService> entry : istioServiceMap.entrySet()) {
            List<IstioEndpoint> istioEndpoints = entry.getValue().getHosts();
            Map<String, LocalityLbEndpoints.Builder> llbEndpointsBuilder = new HashMap<>(istioEndpoints.size());
            
            int port = entry.getValue().getPorts().get(0);
            String name = buildClusterName(TrafficDirection.OUTBOUND, "",
                    entry.getKey() + istioConfig.getDomainSuffix(), port);
            
            for (IstioEndpoint istioEndpoint : istioEndpoints) {
                String label = istioEndpoint.getStringLocality();
                LbEndpoint lbEndpoint = istioEndpoint.getLbEndpoint();
                
                if (!llbEndpointsBuilder.containsKey(label)) {
                    LocalityLbEndpoints.Builder llbEndpointBuilder = LocalityLbEndpoints.newBuilder()
                            .setLocality(istioEndpoint.getLocality()).addLbEndpoints(lbEndpoint);
                    llbEndpointsBuilder.put(label, llbEndpointBuilder);
                } else {
                    llbEndpointsBuilder.get(label).addLbEndpoints(lbEndpoint);
                }
            }
    
            List<LocalityLbEndpoints> listlle = new ArrayList<>();
            for (LocalityLbEndpoints.Builder builder : llbEndpointsBuilder.values()) {
                int weight = 0;
                for (LbEndpoint lbEndpoint : builder.getLbEndpointsList()) {
                    weight += lbEndpoint.getLoadBalancingWeight().getValue();
                }
                LocalityLbEndpoints lle = builder.setLoadBalancingWeight(UInt32Value.newBuilder().setValue(weight)).build();
                listlle.add(lle);
            }
    
            ClusterLoadAssignment cla = ClusterLoadAssignment.newBuilder().setClusterName(name).addAllEndpoints(listlle).build();
            Any any = Any.newBuilder().setValue(cla.toByteString()).setTypeUrl(ENDPOINT_TYPE).build();
            result.add(any);
        }
        
        return result;
    }
}
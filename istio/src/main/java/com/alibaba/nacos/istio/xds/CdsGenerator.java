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
import com.alibaba.nacos.istio.model.IstioService;
import com.google.protobuf.Any;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.Http2ProtocolOptions;
import io.envoyproxy.envoy.config.core.v3.TrafficDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alibaba.nacos.istio.api.ApiConstants.CLUSTER_TYPE;
import static com.alibaba.nacos.istio.util.IstioCrdUtil.buildClusterName;

/**
 * CdsGenerator.
 * @author RocketEngine26
 */
public final class CdsGenerator implements ApiGenerator<Any> {
    
    private static volatile CdsGenerator singleton = null;
    
    public static CdsGenerator getInstance() {
        if (singleton == null) {
            synchronized (ServiceEntryXdsGenerator.class) {
                if (singleton == null) {
                    singleton = new CdsGenerator();
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
            int port = entry.getValue().getPorts().get(0);
            String name = buildClusterName(TrafficDirection.OUTBOUND, "",
                    entry.getKey() + istioConfig.getDomainSuffix(), port);
    
            Cluster cluster = Cluster.newBuilder().setName(name).setType(Cluster.DiscoveryType.EDS)
                    .setHttp2ProtocolOptions(Http2ProtocolOptions.newBuilder().build()).build();
            
            result.add(Any.newBuilder().setValue(cluster.toByteString()).setTypeUrl(CLUSTER_TYPE).build());
        }
        
        return result;
    }
}


/*
 *
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

package com.alibaba.nacos.istio.model;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.google.protobuf.UInt32Value;
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.Locality;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**.
 * @Author RocketEngine26
 * @Date 2022/8/9 10:29
 */
public class IstioEndpoint {
    //TODO:serviceAccount,hostName,workloadName,serviceName
    private Map<String, String> labels;
    
    private String adder;
    
    private LbEndpoint lbEndpoint;
    
    private String serviceAccount;
    
    private Locality locality;
    
    private int port;
    
    private int weight;
    
    private String namespace;
    
    private String groupName;
    
    private String serviceName;
    
    private String clusterName;
    
    private boolean healthy;
    
    private boolean ephemeral;
    
    private boolean enabled;
    
    public IstioEndpoint(Instance instance, IstioService istioService) {
        this.labels = instance.getMetadata();
        this.adder = instance.getIp();
        //instance.serviceName:group@@serviceName
        this.port = instance.getPort();
        this.weight = (int) instance.getWeight();
        this.namespace = istioService.getNamespace();
        this.groupName = istioService.getGroupName();
        this.serviceName = istioService.getName();
        this.clusterName = StringUtils.isNotEmpty(instance.getClusterName()) ? instance.getClusterName() : "";
        this.healthy = instance.isHealthy();
        this.ephemeral = instance.isEphemeral();
        this.enabled = instance.isEnabled();
        
        buildLocality();
    }
    
    /**
     * description:buildLocality.
     * @param: []
     * @return: void
     */
    private void buildLocality() {
        String region = this.labels.getOrDefault("region", "");
        String zone = this.labels.getOrDefault("zone", "");
        String subzone = this.labels.getOrDefault("subzone", "");
    
        this.locality = Locality.newBuilder().setRegion(region).setZone(zone).setSubZone(subzone).build();
    }
    
    /**
     * description:buildLbEndpoint.
     * @param: []
     * @return: void
     */
    private LbEndpoint buildLbEndpoint() {
        Address adder = Address.newBuilder().setSocketAddress(SocketAddress.newBuilder().setAddress(this.adder)
                .setPortValue(this.port).setProtocol(SocketAddress.Protocol.TCP).build()).build();
        this.lbEndpoint = LbEndpoint.newBuilder().setLoadBalancingWeight(UInt32Value.newBuilder().setValue(
                this.weight)).setEndpoint(Endpoint.newBuilder().setAddress(adder).build()).build();
        
        return this.lbEndpoint;
    }
    
    public Map<String, String> getLabels() {
        return labels;
    }
    
    public String getAdder() {
        return adder;
    }
    
    public LbEndpoint getLbEndpoint() {
        return buildLbEndpoint();
    }
    
    public String getServiceAccount() {
        return serviceAccount;
    }
    
    public String getStringLocality() {
        return locality.getRegion() + "." +  locality.getZone() + "." + locality.getSubZone();
    }
    
    public Locality getLocality() {
        return locality;
    }
    
    public int getPort() {
        return port;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public String getClusterName() {
        return clusterName;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public boolean isEphemeral() {
        return ephemeral;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}

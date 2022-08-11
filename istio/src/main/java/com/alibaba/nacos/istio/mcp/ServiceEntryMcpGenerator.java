/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
 */

package com.alibaba.nacos.istio.mcp;

import com.alibaba.nacos.istio.api.ApiGenerator;
import com.alibaba.nacos.istio.common.ResourceSnapshot;
import com.alibaba.nacos.istio.misc.IstioConfig;
import com.alibaba.nacos.istio.model.IstioEndpoint;
import com.alibaba.nacos.istio.model.IstioService;
import com.alibaba.nacos.istio.model.ServiceEntryWrapper;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import istio.mcp.v1alpha1.MetadataOuterClass;
import istio.mcp.v1alpha1.ResourceOuterClass.Resource;
import istio.networking.v1alpha3.GatewayOuterClass;
import istio.networking.v1alpha3.ServiceEntryOuterClass;
import istio.networking.v1alpha3.WorkloadEntryOuterClass;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.alibaba.nacos.istio.api.ApiConstants.SERVICE_ENTRY_PROTO;
import static com.alibaba.nacos.istio.util.IstioCrdUtil.ISTIO_HOSTNAME;
import static com.alibaba.nacos.istio.util.IstioCrdUtil.VALID_LABEL_KEY_FORMAT;
import static com.alibaba.nacos.istio.util.IstioCrdUtil.VALID_LABEL_VALUE_FORMAT;

/**.
 * @author special.fy
 */
public class ServiceEntryMcpGenerator implements ApiGenerator<Resource> {

    private List<ServiceEntryWrapper> serviceEntries;

    private static volatile ServiceEntryMcpGenerator singleton = null;

    public static ServiceEntryMcpGenerator getInstance() {
        if (singleton == null) {
            synchronized (ServiceEntryMcpGenerator.class) {
                if (singleton == null) {
                    singleton = new ServiceEntryMcpGenerator();
                }
            }
        }
        return singleton;
    }

    @Override
    public List<Resource> generate(ResourceSnapshot resourceSnapshot) {
        List<Resource> result = new ArrayList<>();
        IstioConfig istioConfig = resourceSnapshot.getIstioConfig();
        Map<String, IstioService> serviceInfoMap = resourceSnapshot.getIstioContext().getIstioServiceMap();

        for (String serviceName : serviceInfoMap.keySet()) {
            ServiceEntryWrapper serviceEntryWrapper = buildServiceEntry(serviceName, istioConfig
                    .getDomainSuffix(), serviceInfoMap.get(serviceName));
            if (serviceEntryWrapper != null) {
                serviceEntries.add(serviceEntryWrapper);
            }
        }

        for (ServiceEntryWrapper serviceEntryWrapper : serviceEntries) {
            MetadataOuterClass.Metadata metadata = serviceEntryWrapper.getMetadata();
            ServiceEntryOuterClass.ServiceEntry serviceEntry = serviceEntryWrapper.getServiceEntry();

            Any any = Any.newBuilder().setValue(serviceEntry.toByteString()).setTypeUrl(SERVICE_ENTRY_PROTO).build();

            result.add(Resource.newBuilder().setBody(any).setMetadata(metadata).build());
        }

        return result;
    }

    /**
     * description:buildServiceEntry.
     * @param: [serviceName, domainSuffix, istioService]
     * @return: com.alibaba.nacos.istio.model.ServiceEntryWrapper
     */
    public static ServiceEntryWrapper buildServiceEntry(String serviceName, String domainSuffix, IstioService istioService) {
        if (istioService.getHosts().isEmpty()) {
            return null;
        }

        ServiceEntryOuterClass.ServiceEntry.Builder serviceEntryBuilder = ServiceEntryOuterClass.ServiceEntry
                .newBuilder().setResolution(ServiceEntryOuterClass.ServiceEntry.Resolution.STATIC)
                .setLocation(ServiceEntryOuterClass.ServiceEntry.Location.MESH_INTERNAL);

        int port = 0;
        String protocol = "http";
        String hostname = serviceName;

        for (IstioEndpoint istioEndpoint : istioService.getHosts()) {
            if (port == 0) {
                port = istioEndpoint.getPort();
            }

            if (StringUtils.isNotEmpty(istioEndpoint.getLabels().get("protocol"))) {
                protocol = istioEndpoint.getLabels().get("protocol");

                if (protocol.equals("triple") || protocol.equals("tri")) {
                    protocol = "grpc";
                }
            }

            String metaHostname = istioEndpoint.getLabels().get(ISTIO_HOSTNAME);
            if (StringUtils.isNotEmpty(metaHostname)) {
                hostname = metaHostname;
            }

            if (!istioEndpoint.isHealthy() || !istioEndpoint.isEnabled()) {
                continue;
            }

            Map<String, String> metadata = new HashMap<>(1 << 3);
            if (StringUtils.isNotEmpty(istioEndpoint.getClusterName())) {
                metadata.put("cluster", istioEndpoint.getClusterName());
            }

            for (Map.Entry<String, String> entry : istioEndpoint.getLabels().entrySet()) {
                if (!Pattern.matches(VALID_LABEL_KEY_FORMAT, entry.getKey())) {
                    continue;
                }
                if (!Pattern.matches(VALID_LABEL_VALUE_FORMAT, entry.getValue())) {
                    continue;
                }
                metadata.put(entry.getKey().toLowerCase(), entry.getValue());
            }

            WorkloadEntryOuterClass.WorkloadEntry workloadEntry = WorkloadEntryOuterClass.WorkloadEntry.newBuilder()
                    .setAddress(istioEndpoint.getAdder()).setWeight((int) istioEndpoint.getWeight())
                    .putAllLabels(metadata).putPorts(protocol, istioEndpoint.getPort()).build();
            serviceEntryBuilder.addEndpoints(workloadEntry);
        }

        serviceEntryBuilder.addHosts(hostname + "." + domainSuffix).addPorts(
                GatewayOuterClass.Port.newBuilder().setNumber(port).setName(protocol).setProtocol(protocol.toUpperCase()).build());
        ServiceEntryOuterClass.ServiceEntry serviceEntry = serviceEntryBuilder.build();

        Date createTimestamp = istioService.getCreateTimeStamp();
        MetadataOuterClass.Metadata metadata = MetadataOuterClass.Metadata.newBuilder()
                .setName(istioService.getNamespace() + "/" + serviceName)
                .putAnnotations("virtual", "1")
                .putLabels("registryType", "nacos")
                .setCreateTime(Timestamp.newBuilder().setSeconds(createTimestamp.getTime() / 1000).build())
                .setVersion(String.valueOf(istioService.getRevision())).build();

        return new ServiceEntryWrapper(metadata, serviceEntry);
    }
}

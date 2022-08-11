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

package com.alibaba.nacos.istio.util;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.naming.core.v2.pojo.Service;
import io.envoyproxy.envoy.config.core.v3.TrafficDirection;
import istio.networking.v1alpha3.WorkloadEntryOuterClass.WorkloadEntry;

/**.
 * @author special.fy
 */
public class IstioCrdUtil {

    public static final String VALID_DEFAULT_GROUP_NAME = "DEFAULT-GROUP";

    public static final String ISTIO_HOSTNAME = "istio.hostname";

    public static final String VALID_LABEL_KEY_FORMAT = "^([a-zA-Z0-9](?:[-a-zA-Z0-9]*[a-zA-Z0-9])?"
            + "(?:\\.[a-zA-Z0-9](?:[-a-zA-Z0-9]*[a-zA-Z0-9])?)*/)?((?:[A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])$";

    public static final String VALID_LABEL_VALUE_FORMAT = "^((?:[A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?$";

    public static String buildClusterName(TrafficDirection direction, String clusterName, String hostName, int port) {
        return direction.toString() + "." + port + "." + clusterName + "." + hostName;
    }

    /**
     * description:buildLocalityName.
     * @param: [workloadEntry]
     * @return: java.lang.String
     */
    public static String buildLocalityName(WorkloadEntry workloadEntry) {
        String region = workloadEntry.getLabelsOrDefault("region", "");
        String zone = workloadEntry.getLabelsOrDefault("zone", "");
        String subzone = workloadEntry.getLabelsOrDefault("subzone", "");

        return region + "." + zone + "." + subzone;
    }

    /**
     * description:buildServiceName.
     *
     * @param: [service]
     * @return: java.lang.String
     */
    public static String buildServiceName(Service service) {
        String group =
                !Constants.DEFAULT_GROUP.equals(service.getGroup()) ? service.getGroup() : VALID_DEFAULT_GROUP_NAME;

        // DEFAULT_GROUP is invalid for istio,because the istio host only supports: [0-9],[A-Z],[a-z],-,*
        return service.getName() + "." + group + "." + service.getNamespace();
    }
}
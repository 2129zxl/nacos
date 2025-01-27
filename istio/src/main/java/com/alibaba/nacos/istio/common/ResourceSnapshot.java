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

package com.alibaba.nacos.istio.common;

import com.alibaba.nacos.istio.misc.IstioConfig;
import com.alibaba.nacos.istio.model.IstioContext;
import com.alibaba.nacos.istio.model.IstioService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**.
 * @author special.fy
 */
public class ResourceSnapshot {
    private static AtomicLong versionSuffix = new AtomicLong(0);

    private final IstioContext istioContext;

    private IstioConfig istioConfig;

    private boolean isCompleted;

    private String version;

    public ResourceSnapshot() {
        isCompleted = false;
        istioContext = new IstioContext(new ConcurrentHashMap<String, IstioService>(16));
    }

    /**
     * description:initResourceSnapshot.
     * @param: [manager]
     * @return: void
     */
    public synchronized void initResourceSnapshot(NacosResourceManager manager) {
        if (isCompleted) {
            return;
        }

        initIstioContext(manager);

        generateVersion();

        isCompleted = true;
    }

    private void generateVersion() {
        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());
        version = time + "/" + versionSuffix.getAndIncrement();
    }

    private void initIstioContext(NacosResourceManager manager) {
        istioContext.setIstioServiceMap(manager.services());
    }

    public IstioContext getIstioContext() {
        return istioContext;
    }

    public IstioConfig getIstioConfig() {
        return istioConfig;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public String getVersion() {
        return version;
    }
}

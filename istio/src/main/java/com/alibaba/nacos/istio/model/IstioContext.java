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

import java.util.Map;

/**.
 * @Author RocketEngine26
 * @Date 2022/8/9 16:26
 */
public class IstioContext {
    private Map<String, IstioService> istioServiceMap;
    
    public IstioContext(Map<String, IstioService> istioServiceMap) {
        this.istioServiceMap = istioServiceMap;
    }
    
    public Map<String, IstioService> getIstioServiceMap() {
        return istioServiceMap;
    }
    
    public void setIstioServiceMap(Map<String, IstioService> istioServiceMap) {
        this.istioServiceMap = istioServiceMap;
    }
}

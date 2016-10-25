/*
 *
 *  * Copyright 2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package leap.lang.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface MBeanExporter {

    /**
     * Returns the {@link MBeanServer} used by this exporter.
     */
    MBeanServer getServer();

    /**
     * Returns the {@link ObjectName} of the given string name.
     */
    ObjectName createObjectName(String name);

    /**
     * Export the bean as jmx managed bean.
     */
    void export(String name, Object bean);

    /**
     * Export the bean as jmx managed bean.
     */
    void export(ObjectName name, Object bean);

    /**
     * Unexport all the beans exported by this exporter.
     */
    void unexportAll();

}
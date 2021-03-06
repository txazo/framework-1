/*
 * Copyright 2016 the original author or authors.
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
package leap.lang.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link HashSet} wraps the immutable view of itself.
 */
public class ImvHashSet<E> extends HashSet<E> implements ImvSet<E> {

    private Set<E> immutableView;

    public ImvHashSet() {
        super();
    }

    public ImvHashSet(Collection<? extends E> c) {
        super(c);
    }

    public ImvHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ImvHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public Set<E> getImmutableView() {
        if(null == immutableView) {
            immutableView = Collections.unmodifiableSet(this);
        }
        return immutableView;
    }
}
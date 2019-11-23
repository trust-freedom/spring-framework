/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * Allows for custom modification of an application context's bean definitions,
 * adapting the bean property values of the context's underlying bean factory.
 *
 * <p>Application contexts can auto-detect BeanFactoryPostProcessor beans in
 * their bean definitions and apply them before any other beans get created.
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context.
 *
 * <p>See PropertyResourceConfigurer and its concrete implementations
 * for out-of-the-box solutions that address such configuration needs.
 *
 * <p>A BeanFactoryPostProcessor may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 *
 * @author Juergen Hoeller
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 */
public interface BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean factory after its standardE
	 * initialization.
	 * 修改ApplicationContext内部的BeanFactory，在BeanFactory的标准初始化后
	 *
	 * All bean definitions will have been loaded, but no beans will have been instantiated yet.
	 * 执行时机：所有的bean定义都已经被加载，但还没有bean被初始化（即bean的实例还未创建）
	 *
	 * 自己：
	 * 其实BeanDefinitionRegistryPostProcessor 和 BeanFactoryPostProcessor是一先一后调用的
	 * 这里之所以说调用时机是所有标准bean定义将要被加载，是因为BeanDefinitionRegistryPostProcessor就是用来注册修改bean定义的后置处理器
	 * 而等运行完BeanDefinitionRegistryPostProcessor，所有bean定义就成型了，执行BeanFactoryPostProcessor时就是“所有的bean定义都已经被加载”
	 *
	 * This allows for overriding or adding properties even to eager-initializing beans.
	 * 这将允许覆盖或添加bean的属性，即使是eager-initializing beans（立即加载的bean？？）
	 *
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}

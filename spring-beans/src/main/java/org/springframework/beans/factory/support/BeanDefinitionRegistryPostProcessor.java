/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * Extension to the standard {@link BeanFactoryPostProcessor} SPI, allowing for
 * the registration of further bean definitions <i>before</i> regular
 * BeanFactoryPostProcessor detection kicks in. In particular,
 * BeanDefinitionRegistryPostProcessor may register further bean definitions
 * which in turn define BeanFactoryPostProcessor instances.
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 * @see org.springframework.context.annotation.ConfigurationClassPostProcessor
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * Modify the application context's internal bean definition registry after its
	 * standard initialization.
	 * 修改application context内部的bean definition registry，在其标准初始化之后（标准初始化？？）
	 *
	 * All regular bean definitions will have been loaded, but no beans will have been instantiated yet.
	 * 执行时机：所有标准的bean定义将要被加载（也就是还没加载），但是还没有bean的实例被创建
	 *
	 * 自己：
	 * 其实BeanDefinitionRegistryPostProcessor 和 BeanFactoryPostProcessor是一先一后调用的
	 * 这里之所以说调用时机是所有标准bean定义将要被加载，是因为BeanDefinitionRegistryPostProcessor就是用来注册修改bean定义的后置处理器
	 * 而等运行完BeanDefinitionRegistryPostProcessor，所有bean定义就成型了，执行BeanFactoryPostProcessor时就是“所有的bean定义都已经被加载”
	 *
	 * This allows for adding further bean definitions before the next post-processing phase kicks in.
	 * 这允许在下一个后处理阶段开始之前添加更多的bean定义
	 *
	 * @param registry the bean definition registry used by the application context  applicationContext中的bean定义注册中心
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException;

}

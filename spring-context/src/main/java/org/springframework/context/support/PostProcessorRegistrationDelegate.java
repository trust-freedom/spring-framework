/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 * 代理AbstractApplicationContext的(BeanFactory)后置处理器的处理
 * 包含BeanFactoryPostProcessor、BeanDefinitionRegistryPostProcessor的调用，还有BeanPostProcessor的注册
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
class PostProcessorRegistrationDelegate {

	/**
	 * 先调用BeanDefinitionRegistryPostProcessor，再调用BeanFactoryPostProcessor
	 * @param beanFactory
	 * @param beanFactoryPostProcessors
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		// 先执行BeanDefinitionRegistryPostProcessors
		Set<String> processedBeans = new HashSet<String>();

		/**
		 * 如果beanFactory是BeanDefinitionRegistry接口类型的，即具有注册bean定义的能力
		 * 且参数传进来的beanFactoryPostProcessors中有BeanDefinitionRegistryPostProcessor的实现类(能够提供注册bean定义)就调用
		 * 就先调用BeanDefinitionRegistryPostProcessor，再调用BeanFactoryPostProcessor
		 * 否则直接调用BeanFactoryPostProcessor
		 *
		 * 执行所有参数传进来的BeanDefinitionRegistryPostProcessor
		 * 是通过AbstractApplicationContext#getBeanFactoryPostProcessors()获取的，应该是之前的beanFactory标准初始化步骤中设置的BeanDefinitionRegistryPostProcessor
		 * 对于AnnotationConfigApplicationContext来说，没有设置，可能对其它xxxApplicationContext会设置
		 */
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new LinkedList<BeanDefinitionRegistryPostProcessor>();

			// 以下调用都是针对参数传进来的beanFactoryPostProcessors集合的
			// 如果是BeanDefinitionRegistryPostProcessor，调用其方法，并给后置处理器分类
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {//如果是BeanDefinitionRegistryPostProcessor
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);//调用postProcessBeanDefinitionRegistry()方法
					registryProcessors.add(registryProcessor); //添加到registryProcessors集合，
				}
				else {
					regularPostProcessors.add(postProcessor); //添加到regularPostProcessors集合，常规的后置处理器
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<BeanDefinitionRegistryPostProcessor>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// 首先，调用beanFactory中实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessors
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {//如果BeanDefinitionRegistryPostProcessor实现了PriorityOrdered接口
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));//使用了beanFactory.getBean()实例化BeanDefinitionRegistryPostProcessor，否则bean定义后置处理器只是出于beanDefinition阶段
					processedBeans.add(ppName);  //添加到已执行的BeanFactoryPostProcessor
					                             //因为BeanDefinitionRegistryPostProcessor是BeanFactoryPostProcessor的子接口，也有其方法
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);  //排序
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);//调用BeanDefinitionRegistryPostProcessor集合
			currentRegistryProcessors.clear();  //清空currentRegistryProcessors

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 其次，调用beanFactory中实现了Ordered接口的BeanDefinitionRegistryPostProcessors
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {//如果还未被调用，且BeanDefinitionRegistryPostProcessor实现了Ordered接口
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);  //排序
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);//调用BeanDefinitionRegistryPostProcessor集合
			currentRegistryProcessors.clear();  //清空currentRegistryProcessors

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 最后，调用所有其他BeanDefinitionRegistryPostProcessors，直到没有其他BeanDefinitionRegistryPostProcessors出现
			// 会从beanFactory中按照类型获取BeanDefinitionRegistryPostProcessor，并调用
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {  //如果没有被执行过
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);  //排序
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);//执行BeanDefinitionRegistryPostProcessors的postProcessBeanDefinitionRegistry()方法
				currentRegistryProcessors.clear();  //清空currentRegistryProcessors
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 现在，调用BeanFactory后置处理器的postProcessBeanFactory()方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);//先执行实现了BeanDefinitionRegistryPostProcessors接口的postProcessBeanFactory()
			                                                                 //包括参数中传进来的 和 从beanFactory中获取的
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);//再执行参数中传进来的其它常规BeanFactoryPostProcessor
		}
		// 如果beanFactory不是BeanDefinitionRegistry接口类型的，那么不用判断BeanDefinitionRegistryPostProcessor，直接执行BeanFactoryPostProcessor，为什么还需要else，而不是直接用下面的代码逻辑？？
		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}


		/**
		 * 以下逻辑是针对从beanFactory中获取的BeanFactoryPostProcessor集合
		 * （没有针对beanFactory中的BeanDefinitionRegistryPostProcessors集合的调用）
		 */

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
		/**
		 * 遍历所有的BeanFactoryPostProcessor的实例bean的名称
		 * 判断其是否实现了PriorityOrdered接口或Ordered接口，从而被放入不同的集合
		 */
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {//使用beanFactory.isTypeMatch()判断类型是否匹配
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		// 首先，调用实现了PriorityOrdered接口的BeanFactoryPostProcessors
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);  //传入priorityOrderedPostProcessors集合，beanFactory用户获得排序比较器OrderComparator
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);  //调用所有priorityOrderedPostProcessors

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		// 其次，调用实现了Ordered接口的BeanFactoryPostProcessors
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : orderedPostProcessorNames) {  //构造orderedPostProcessors集合，根据名称从beanFactory中获取
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);//调用所有orderedPostProcessors

		// Finally, invoke all other BeanFactoryPostProcessors.
		// 最后，调用所有其它BeanFactoryPostProcessors
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {  //构造nonOrderedPostProcessors集合，根据名称从beanFactory中获取
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);  //调用所有其它普通的BeanFactoryPostProcessors

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}


	/**
	 * 注册BeanPostProcessors
	 * @param beanFactory
	 * @param applicationContext
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		/**
		 * getBeanNamesForType()应该是从所有beanDefinition中获取beanName，由于此时bean还未初始化，那么这些就是还没创建的BeanPostProcessors
		 * 0 = "org.springframework.context.annotation.internalAutowiredAnnotationProcessor"
		 * 1 = "org.springframework.context.annotation.internalRequiredAnnotationProcessor"
		 * 2 = "org.springframework.context.annotation.internalCommonAnnotationProcessor"
		 * beanFactory的beanDefinitionNames属性中有上面这3个
		 */
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		// 注册一个BeanPostProcessorChecker用于记录BeanPostProcessor初始化期间是否有bean被创建
		// 这种bean就不适用于所有BeanPostProcessors，因为在BeanPostProcessors还未创建时这个bean就已经创建了，而BeanPostProcessors就是拦截bean创建的
		// beanFactory.getBeanPostProcessorCount()可以理解为已经注册的BeanPostProcessors？？加上BeanPostProcessorChecker自己，再加上还未注册的BeanPostProcessors？？
		// beanFactory.getBeanPostProcessorCount()实际运行中为：
		// 0 = {ApplicationContextAwareProcessor@1561}
		// 1 = {ApplicationListenerDetector@1562}
		// 2 = {ConfigurationClassPostProcessor$ImportAwareBeanPostProcessor@1563}
		// 其中前两个是在beanFactory的标准初始化阶段的prepareBeanFactory(beanFactory)时被添加到beanFactory的
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));


		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 按照排序分组
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) { //实现了PriorityOrdered接口的BeanPostProcessors
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);

				if (pp instanceof MergedBeanDefinitionPostProcessor) { //如果是MergedBeanDefinitionPostProcessor类型
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) { //实现了Ordered接口的BeanPostProcessors
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName); //没有顺序的BeanPostProcessors
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		// 首先，注册实现了PriorityOrdered的BeanPostProcessors
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors); //向beanFactory中注册BeanPostProcessors【beanFactory.addBeanPostProcessor(postProcessor)】

		// Next, register the BeanPostProcessors that implement Ordered.
		// 其次，注册实现了Ordered的BeanPostProcessors
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);

			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors); //向beanFactory中注册BeanPostProcessors【beanFactory.addBeanPostProcessor(postProcessor)】

		// Now, register all regular BeanPostProcessors.
		// 现在，注册所有常规的BeanPostProcessors
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);

			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		// 最后，重新注册所有内部的BeanPostProcessors，就是上面迭代时找到的MergedBeanDefinitionPostProcessor
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 重新注册ApplicationListenerDetector这个后置处理器，可以让它跑到后置处理器列表的最后一个
		// ApplicationListenerDetector是用于探测bean是否是ApplicationListener，是就applicationContext.addApplicationListener()
		// 已经在beanFactory的标准初始化阶段的prepareBeanFactory(beanFactory)，添加过了
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		Collections.sort(postProcessors, comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.参数中的BeanDefinitionRegistry其实就是一个具有bean定义注册功能的beanFactory【DefaultListableBeanFactory】
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		//逐个回调postProcessor.postProcessBeanFactory()，并传入beanFactory
		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 * 向beanFactory中注册BeanPostProcessor
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			//当前bean不是BeanPostProcessor，不是基础设施bean，且beanFactory中的BeanPostProcessor数量还未注册到位，达到TargetCount
			//此时创建的bean就不是所有BeanPostProcessors都能作用于它，因为它在BeanPostProcessors还没创建/注册完毕前就已经创建了
			if (bean != null && !(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +  //不适合所有BeanPostProcessors处理
							"(for example: not eligible for auto-proxying)");  //举例：不适合自动代理
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}

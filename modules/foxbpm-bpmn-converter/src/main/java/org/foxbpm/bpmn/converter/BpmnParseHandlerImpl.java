/**
 * Copyright 1996-2014 FoxBPM ORG.
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
 * @author kenshin
 */
package org.foxbpm.bpmn.converter;

import java.io.ByteArrayInputStream;
import java.net.URL;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.di.BpmnDiPackage;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.dd.dc.DcPackage;
import org.eclipse.dd.di.DiPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.foxbpm.engine.ProcessEngineManagement;
import org.foxbpm.engine.exception.ExceptionCode;
import org.foxbpm.engine.exception.FixFlowClassLoadingException;
import org.foxbpm.engine.impl.entity.ProcessDefinitionEntity;
import org.foxbpm.engine.impl.util.ReflectUtil;
import org.foxbpm.engine.modelparse.ProcessModelParseHandler;
import org.foxbpm.model.bpmn.foxbpm.FoxBPMPackage;

public class BpmnParseHandlerImpl implements ProcessModelParseHandler {

	public ProcessDefinitionEntity createProcessDefinition(String processId,Object processFile) {
		

		if (bytesObject != null) {
			byte[] bytes = (byte[]) bytesObject;
			ResourceSet resourceSet = getResourceSet();
			String fixflowFilePath =  ProcessEngineManagement.getDefaultProcessEngine().getProcessEngineConfiguration().getNoneTemplateFilePath();
			URL url = ReflectUtil.getResource(fixflowFilePath);
			if(url == null){
				throw new FixFlowClassLoadingException(ExceptionCode.CLASSLOAD_EXCEPTION_FILENOTFOUND,fixflowFilePath);
			}
			String filePath = url.toString();
			Resource ddddResource = null;
			try {
				if (!filePath.startsWith("jar")) {
					filePath = java.net.URLDecoder.decode(url.getFile(),"utf-8");
					ddddResource = resourceSet.createResource(URI.createFileURI(filePath));
				} else {
					ddddResource = resourceSet.createResource(URI.createURI(filePath));
				}
				ddddResource.load(new ByteArrayInputStream(bytes), null);
			} catch (Exception e) {
				throw new FixFlowClassLoadingException(ExceptionCode.CLASSLOAD_EXCEPTION, e);
			}
			DefinitionsBehavior definitions = (DefinitionsBehavior) ddddResource.getContents().get(0).eContents().get(0);
			definitions.setProcessId(processId);

			for (RootElement rootElement : definitions.getRootElements()) {
				if (rootElement instanceof ProcessDefinitionBehavior) {
					ProcessDefinitionBehavior processObj = (ProcessDefinitionBehavior) rootElement;
					if (processObj.getProcessDefinitionKey().equals(processKey)) {
						processDefinition = (ProcessDefinitionBehavior) rootElement;
						break;
					}
				}
			}
			processDefinition.setDefinitions(definitions);
			// 加载事件定义.
			loadEvent(processDefinition);
			// 加载数据变量
			loadVariable(processDefinition);
			// 设置FlowNode元素的子流程
			loadSubProcess(processDefinition);
			processDefinition.persistentInit(processDataMap);
			deploymentCache.addProcessDefinition(processDefinition);
			return processDefinition;

		}
		
		
	}
	
	
	private Process createProcess(String processId,byte[] bytes){
		
		ResourceSet resourceSet = getResourceSet();
		String fixflowFilePath =  ProcessEngineManagement.getDefaultProcessEngine().getProcessEngineConfiguration().getNoneTemplateFilePath();
		URL url = ReflectUtil.getResource(fixflowFilePath);
		if(url == null){
			throw new FixFlowClassLoadingException(ExceptionCode.CLASSLOAD_EXCEPTION_FILENOTFOUND,fixflowFilePath);
		}
		String filePath = url.toString();
		Resource ddddResource = null;
		try {
			if (!filePath.startsWith("jar")) {
				filePath = java.net.URLDecoder.decode(url.getFile(),"utf-8");
				ddddResource = resourceSet.createResource(URI.createFileURI(filePath));
			} else {
				ddddResource = resourceSet.createResource(URI.createURI(filePath));
			}
			ddddResource.load(new ByteArrayInputStream(bytes), null);
		} catch (Exception e) {
			throw new FixFlowClassLoadingException(ExceptionCode.CLASSLOAD_EXCEPTION, e);
		}
		Definitions definitions = (Definitions) ddddResource.getContents().get(0).eContents().get(0);
		for (RootElement rootElement : definitions.getRootElements()) {
			if (rootElement instanceof Process) {
				Process processObj = (Process) rootElement;
				
				BpmnModelUtil.getExtensionAttribute(processObj, FoxBPMPackage.Literals.DOCUMENT_ROOT__DBID);
				
				if (processObj.getProcessDefinitionKey().equals(processId)) {
					processDefinition = (ProcessDefinitionBehavior) rootElement;
					return processObj;
				}
			}
		}
		
	}
	
	private ResourceSet getResourceSet() {
		ResourceSet resourceSet = new ResourceSetImpl();
		(EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/BPMN/20100524/MODEL", Bpmn2Package.eINSTANCE);
		(EPackage.Registry.INSTANCE).put("http://www.foxbpm.org/foxbpm", FoxBPMPackage.eINSTANCE);
		(EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/DD/20100524/DI", DiPackage.eINSTANCE);
		(EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/DD/20100524/DC", DcPackage.eINSTANCE);
		(EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/BPMN/20100524/DI", BpmnDiPackage.eINSTANCE);
		FoxBPMPackage.eINSTANCE.eClass();
		FoxBPMPackage xxxPackage = FoxBPMPackage.eINSTANCE;
		EPackage.Registry.INSTANCE.put(xxxPackage.getNsURI(), xxxPackage);
		Bpmn2ResourceFactoryImpl ddd = new Bpmn2ResourceFactoryImpl();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("fixflow", ddd);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("bpmn", ddd);
		resourceSet.getPackageRegistry().put(xxxPackage.getNsURI(), xxxPackage);
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpmn", ddd);
		return resourceSet;
	}

}

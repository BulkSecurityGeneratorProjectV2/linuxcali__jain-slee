package org.mobicents.slee.container.deployment.profile;

import java.security.PrivilegedActionException;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.NotFoundException;

import javax.slee.SLEEException;
import javax.slee.management.DeploymentException;
import javax.slee.profile.ProfileLocalObject;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.component.ClassPool;
import org.mobicents.slee.container.component.profile.ProfileSpecificationComponent;
import org.mobicents.slee.container.component.profile.ProfileSpecificationDescriptor;
import org.mobicents.slee.container.deployment.ClassUtils;
import org.mobicents.slee.container.deployment.ConcreteClassGeneratorUtils;
import org.mobicents.slee.container.profile.ProfileLocalObjectImpl;
import org.mobicents.slee.container.profile.ProfileObjectImpl;
import org.mobicents.slee.container.security.Utility;

public class ConcreteProfileLocalObjectGenerator {

	private static final Logger logger = Logger.getLogger(ConcreteProfileLocalObjectGenerator.class);
	
	/**
	 * the component to process
	 */
	private final ProfileSpecificationComponent component;

	/**
	 * 
	 * @param component
	 */
	public ConcreteProfileLocalObjectGenerator(ProfileSpecificationComponent component) {
		this.component = component;		
	}

	/**
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void generateProfileLocalConcreteClass() throws Exception {
		
		ProfileSpecificationDescriptor descriptor = component.getDescriptor();
		ClassPool pool = component.getClassPool();
				
		CtClass profileLocalObjectInterface = null;
		CtClass profileLocalObjectImplClass = null;
		try {
			// This is the class we will extend
			profileLocalObjectImplClass = pool.get(ProfileLocalObjectImpl.class.getName());
			// the slee defined interface
			profileLocalObjectInterface = pool.get(ProfileLocalObject.class.getName());	
		}
		catch (NotFoundException nfe) {
			throw new SLEEException("Failed to locate required class for " + component, nfe);
		}

		boolean profileLocalObjectInterfaceProvided = descriptor.getProfileLocalInterface() != null;
		
		String profileLocalInterfaceName = null;
		String profileLocalConcreteClassName = null;
		if (profileLocalObjectInterfaceProvided) {
			profileLocalInterfaceName = descriptor.getProfileLocalInterface().getProfileLocalInterfaceName();
			profileLocalConcreteClassName = profileLocalInterfaceName + "Impl";
		}
		else {
			profileLocalInterfaceName = descriptor.getProfileCMPInterface().getProfileCmpInterfaceName();
			profileLocalConcreteClassName = profileLocalInterfaceName + "_PLO_Impl";
		}

		CtClass profileLocalConcreteClass = null;
		try {
			profileLocalConcreteClass = pool.makeClass(profileLocalConcreteClassName);
		} catch (Exception e) {
			throw new DeploymentException("Failed to create Profile Local Interface implementation class.",e);
		}
		
		CtClass profileLocalInterface = null;
		try {
			profileLocalInterface = pool.get(profileLocalInterfaceName);
		}
		catch (NotFoundException nfe) {
			throw new DeploymentException("Failed to locate user provided local object interface class for " + component, nfe);
		}
		
		// implement only methods from profile cmp and management interface that are also on user defined local object interface
		Map<String, CtMethod> methodsToImplement = ClassUtils.getInterfaceMethodsFromInterface(profileLocalInterface,ClassUtils.getInterfaceMethodsFromInterface(profileLocalObjectInterface));
		
		// Create interface and inheritance links
		CtClass[] presentInterfaces = profileLocalObjectImplClass.getInterfaces();
		CtClass[] targetInterfaces = new CtClass[presentInterfaces.length + 1];
		for (int index = 0; index < presentInterfaces.length; index++) {
			targetInterfaces[index] = presentInterfaces[index];
		}
		targetInterfaces[targetInterfaces.length - 1] = profileLocalInterface;	
		ConcreteClassGeneratorUtils.createInterfaceLinks(profileLocalConcreteClass, targetInterfaces);
		ConcreteClassGeneratorUtils.createInheritanceLink(profileLocalConcreteClass, profileLocalObjectImplClass);
		
		// generate constructor
	    try {
	      CtClass[] constructorParameters = new CtClass[] { pool.get(ProfileObjectImpl.class.getName()) };
	      String constructorBody = "{ super($$); }";
	      CtConstructor constructor = CtNewConstructor.make(constructorParameters, new CtClass[]{}, constructorBody , profileLocalConcreteClass);
	      profileLocalConcreteClass.addConstructor(constructor);
	    } catch (CannotCompileException e) {
	      throw new SLEEException(e.getMessage(),e);
	    }
		
	    // implement methods delegating to an object casted to the profile cmp concrete class generated by SLEE
	    String profileCmpConcreteClassName = component.getProfileCmpConcreteClass().getName();
	    for (CtMethod ctMethod : methodsToImplement.values()) {
			implementMethod(profileLocalConcreteClass, ctMethod,"(("+profileCmpConcreteClassName+")profileObject.getProfileConcrete())");
		}
		// generate class file
		try {
			profileLocalConcreteClass.writeFile(component.getDeploymentDir().getAbsolutePath());			
		}
		catch (Exception e) {
			throw new SLEEException(e.getMessage(), e);
		}
		finally {
			// let go, so that it's not holding subsequent deployments of the same profile component.
			// This would not have been necessary is the ClassPool is not one shared instance in the SLEE,
			// but there is instead a hierarchy mimicing the classloader hierarchy. This also makes
			// our deployer essentially single threaded.
			profileLocalConcreteClass.defrost();
		}
		// load it and store it in the component
		try {
			
			component.setProfileLocalObjectConcreteClass(component.getClassLoader().loadClass(profileLocalConcreteClassName));
		}
		catch (ClassNotFoundException e) {
			throw new SLEEException(e.getMessage(),e);
		}
	}

	private void implementMethod(CtClass concreteClass, CtMethod method, String interceptorAccess) throws Exception {
		// copy method to concrete class
		CtMethod newMethod = CtNewMethod.copy( method, concreteClass, null );
		// generate body

		String body=
			"{" 
			+ " sleeContainer.getTransactionManager().mandateTransaction();" 
			+ "	checkTransaction();"
			+ "	try {" 
			+ "		if(System.getSecurityManager()!=null && profileObject.getProfileTable().getProfileSpecificationComponent().getDescriptor().isIsolateSecurityPermissions()){" 
			+       (method.getReturnType().equals(CtClass.voidType) ? "" : "return ($r)")+Utility.class.getName()+".makeSafeProxyCall("+interceptorAccess+",\""+method.getName()+"\",$sig,$args);"
			+ "		}"
			+ "		else {" 
			+       (method.getReturnType().equals(CtClass.voidType) ? "" : "return ")+ interceptorAccess +'.'+ method.getName()+"($$);" 
			+ "		}"			
			+ "	} catch ("+RuntimeException.class.getName()+" e) {"
			+ "		processRuntimeException(e);"		
			+ "	} catch ("+PrivilegedActionException.class.getName()+" e) {"
			+ "		processPrivilegedActionException(e);"		
			+ "	}"
			+ (method.getReturnType().equals(CtClass.voidType) ? "" : "	throw new "+SLEEException.class.getName()+"(\"bug in code generation, this was added to ensure code has no return here\");")			
			+"}";
		
		if(logger.isTraceEnabled())
		{
			logger.trace("Instrumented method, name:"+method.getName()+", with body:\n"+body);
		}
		
		newMethod.setBody(body);
		// add to concrete class  
		concreteClass.addMethod(newMethod);

		
		
	}
}
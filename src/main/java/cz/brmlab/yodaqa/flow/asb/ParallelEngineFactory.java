package cz.brmlab.yodaqa.flow.asb;

import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.impl.AnalysisEngineFactory_impl;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/** A variant of AnalysisEngineFactory that creates ParallelAnalysisEngine
 * instead of AggregateAnalysisEngine.  The ParallelAnalysisEngine works
 * the same, but uses a modified Analysis Structure Broker MultiThreadASB
 * that uses a thread pool to spread logically parallelizable work).
 *
 * XXX: Ideally, UIMA would rather let us specify the "target class"
 * as part of AnalysisEngineDescription; then, we would not need
 * this class at all. */

public class ParallelEngineFactory extends AnalysisEngineFactory_impl {
	/** Disable automatic creation of pool of independent engines for
	 * the purpose of parallelization.  This means that instead
	 * of N times, the AnalysisEngine will be instantiated just
	 * once, and multiple threads may invoke it independently.
	 * Therefore, methods of this AE should be all synchronized
	 * or use proper locking!  You want to use this for CAS mergers
	 * (which want to collect all CASes from all threads in a single
	 * place), or for AEs that use thread-unsafe static members.
	 *
	 * N.B. This can actually be passed both as a configuration parameter
	 * (passed when creating a pipeline) and internally generated by
	 * ParallelEngineFactory as an engine additional parameter (internal
	 * for AE factory). */
	public static final String PARAM_NO_MULTIPROCESSING = "-no-multiprocessing";

	public Resource produceResource(Class<? extends Resource> aResourceClass, ResourceSpecifier aSpecifier,
			Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
		if (!(aSpecifier instanceof AnalysisEngineDescription))
			return super.produceResource(aResourceClass, aSpecifier, aAdditionalParams);
		AnalysisEngineDescription aEngineSpecifier = (AnalysisEngineDescription) aSpecifier;

		/* We just check whether this would produce an
		 * AggregateAnalysisEngine and do our own thing
		 * in that case; we repeat the super's checks
		 * as they are written there. */
		boolean multiprocessing = (aAdditionalParams != null)
			&& aAdditionalParams.containsKey(AnalysisEngine.PARAM_NUM_SIMULTANEOUS_REQUESTS);
		if (!multiprocessing
		    && aResourceClass.isAssignableFrom(TextAnalysisEngine.class)) {
			ResourceCreationSpecifier spec = (ResourceCreationSpecifier) aSpecifier;
			String frameworkImpl = spec.getFrameworkImplementation();
			if (frameworkImpl != null
			    && frameworkImpl.startsWith(Constants.JAVA_FRAMEWORK_NAME)
			    && !aEngineSpecifier.isPrimitive()) {
				Resource resource = new ParallelAnalysisEngine();
				if (resource.initialize(aSpecifier, aAdditionalParams))
					return resource;
			}
		}

		return super.produceResource(aResourceClass, aSpecifier, aAdditionalParams);
	}

}

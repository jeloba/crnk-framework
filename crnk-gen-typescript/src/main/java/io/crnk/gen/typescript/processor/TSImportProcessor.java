package io.crnk.gen.typescript.processor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.crnk.gen.typescript.model.TSElement;
import io.crnk.gen.typescript.model.TSImport;
import io.crnk.gen.typescript.model.TSParameterizedType;
import io.crnk.gen.typescript.model.TSPrimitiveType;
import io.crnk.gen.typescript.model.TSSource;
import io.crnk.gen.typescript.model.TSType;
import io.crnk.gen.typescript.writer.TSTypeReferenceResolver;

/**
 * Computes Typescript import statements for the given source model.
 */
public class TSImportProcessor implements TSSourceProcessor {

	@Override
	public Set<TSSource> process(Set<TSSource> sources) {
		for (TSSource source : sources) {
			transform(source);
			sortImports(source);
		}
		return sources;

	}

	private static void sortImports(TSSource source) {
		List<TSImport> imports = source.getImports();
		Collections.sort(imports, new Comparator<TSImport>() {
			@Override
			public int compare(TSImport o1, TSImport o2) {
				return o1.getPath().compareTo(o2.getPath());
			}
		});
		for (TSImport importElement : imports) {
			Collections.sort(importElement.getTypeNames());
		}
	}

	private static void transform(TSSource source) {
		TSTypeReferenceResolver refResolver = new TSTypeReferenceResolver();
		refResolver.accept(source);
		for (TSType type : refResolver.getTypes()) {
			processType(source, type);
		}
	}

	private static void processType(TSSource source, TSType type) {
		TSSource refSource = getSource(type);

		// no need for inclusions of primitive types and within same file
		if (type instanceof TSParameterizedType) {
			TSParameterizedType paramType = (TSParameterizedType) type;
			processType(source, paramType.getBaseType());
			for (TSType param : paramType.getParameters()) {
				processType(source, param);
			}
		}
		else if (!(type instanceof TSPrimitiveType || source == refSource)) {
			addImport(refSource, source, type);
		}
	}

	private static void addImport(TSSource refSource, TSSource source, TSType type) {
		String path = computeImportPath(refSource, source);
		TSImport element = source.getImport(path);
		if (element == null) {
			element = new TSImport();
			element.setPath(path);
			source.getImports().add(element);
		}
		element.addTypeName(type.getName());
	}

	private static String computeImportPath(TSSource refSource, TSSource source) {
		if (refSource == null) {
			throw new NullPointerException();
		}
		StringBuilder pathBuilder = new StringBuilder();
		if (!source.getNpmPackage().equals(refSource.getNpmPackage())) {
			appendThirdPartyImport(pathBuilder, refSource);
		}
		else {
			appendRelativeImport(pathBuilder, refSource, source);
		}
		return pathBuilder.toString();
	}

	private static void appendRelativeImport(StringBuilder pathBuilder, TSSource refSource, TSSource source) {
		String[] srcDirs = source.getDirectory() != null ? source.getDirectory().split("\\/") : new String[0];
		String[] refDirs = refSource.getDirectory() != null ? refSource.getDirectory().split("\\/") : new String[0];

		int shared = computeSharedPrefix(srcDirs, refDirs);
		appendParentPath(pathBuilder, srcDirs, shared);
		appendChildPath(pathBuilder, refDirs, shared);
		pathBuilder.append(refSource.getName());
	}

	private static void appendChildPath(StringBuilder pathBuilder, String[] refDirs, int shared) {
		for (int i = shared; i < refDirs.length; i++) {
			pathBuilder.append(refDirs[i]);
			pathBuilder.append("/");
		}
	}

	private static void appendParentPath(StringBuilder pathBuilder, String[] srcDirs, int shared) {
		if (shared == srcDirs.length) {
			pathBuilder.append("./");
		}
		else {
			for (int i = shared; i < srcDirs.length; i++) {
				pathBuilder.append("../");
			}
		}
	}

	private static void appendThirdPartyImport(StringBuilder pathBuilder, TSSource refSource) {
		pathBuilder.append(refSource.getNpmPackage());
		if (refSource.getDirectory() != null) {
			pathBuilder.append("/");
			pathBuilder.append(refSource.getDirectory());
		}
	}

	private static int computeSharedPrefix(String[] srcDirs, String[] refDirs) {
		int n = 0;
		for (int i = 0; i < Math.min(srcDirs.length, refDirs.length); i++) {
			if (srcDirs[i].equals(refDirs[i])) {
				n++;
			}
			else {
				break;
			}
		}
		return n;
	}

	private static TSSource getSource(TSElement element) {
		TSElement parent = element.getParent();
		if (parent == null) {
			return null;
		}
		if (parent instanceof TSSource) {
			return (TSSource) parent;
		}
		return getSource(parent);
	}
}

package io.crnk.gen.typescript.model;

import java.util.ArrayList;
import java.util.List;

public class TSFunction extends TSMember implements TSExportedElement {

	private List<String> statements = new ArrayList<>();

	private List<TSParameter> parameters = new ArrayList<>();

	private boolean exported;

	@Override
	public void accept(TSVisitor visitor) {
		visitor.visit(this);
	}

	public List<String> getStatements() {
		return statements;
	}

	public void setStatements(List<String> statements) {
		this.statements = statements;
	}

	public List<TSParameter> getParameters() {
		return parameters;
	}

	public void setParameters(List<TSParameter> parameters) {
		this.parameters = parameters;
	}

	public void setExported(boolean exported) {
		this.exported = exported;
	}

	@Override
	public boolean isExported() {
		return exported;
	}

	public void addParameter(TSParameter parameter) {
		parameters.add(parameter);
	}
}
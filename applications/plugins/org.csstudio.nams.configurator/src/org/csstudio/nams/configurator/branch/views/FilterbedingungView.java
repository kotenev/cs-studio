package org.csstudio.nams.configurator.branch.views;

import org.csstudio.nams.configurator.branch.composite.FilteredListVarianteA;
import org.csstudio.nams.configurator.branch.composite.FilteredListVarianteC;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class FilterbedingungView extends ViewPart {

	public FilterbedingungView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		//new FilteredListVarianteC(parent, SWT.None);

		new FilteredListVarianteA(parent, SWT.None);
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}

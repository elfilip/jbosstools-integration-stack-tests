package org.jboss.tools.teiid.ui.bot.test.suite;

import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.tools.teiid.ui.bot.test.ImportWizardTest;
import org.jboss.tools.teiid.ui.bot.test.ModelWizardTest;
import org.jboss.tools.teiid.ui.bot.test.TopDownWsdlTest;
import org.jboss.tools.teiid.ui.bot.test.VirtualGroupTutorialTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for all teiid bot tests
 * 
 * @author apodhrad, tsedmik
 */
@SuiteClasses({
	ImportWizardTest.class,
	ModelWizardTest.class,
	TopDownWsdlTest.class,
	VirtualGroupTutorialTest.class
})
@RunWith(RedDeerSuite.class)
public class AllTests {
}

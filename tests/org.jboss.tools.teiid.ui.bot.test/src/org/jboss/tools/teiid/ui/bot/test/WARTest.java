package org.jboss.tools.teiid.ui.bot.test;

import java.util.Properties;

import org.eclipse.swtbot.swt.finder.SWTBotTestCase;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.eclipse.core.resources.AbstractExplorerItem;
import org.jboss.reddeer.eclipse.wst.server.ui.view.Server;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersView;
import org.jboss.reddeer.eclipse.wst.server.ui.view.ServersViewEnums.ServerState;
import org.jboss.reddeer.junit.requirement.inject.InjectRequirement;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.requirements.server.ServerReqState;
import org.jboss.reddeer.swt.condition.ShellWithTextIsActive;
import org.jboss.reddeer.swt.condition.WaitCondition;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.menu.ShellMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.text.DefaultText;
import org.jboss.reddeer.swt.wait.AbstractWait;
import org.jboss.reddeer.swt.wait.TimePeriod;
import org.jboss.reddeer.swt.wait.WaitUntil;
import org.jboss.tools.teiid.reddeer.WAR;
import org.jboss.tools.teiid.reddeer.editor.VDBEditor;
import org.jboss.tools.teiid.reddeer.manager.ConnectionProfileManager;
import org.jboss.tools.teiid.reddeer.manager.ImportManager;
import org.jboss.tools.teiid.reddeer.manager.ModelExplorerManager;
import org.jboss.tools.teiid.reddeer.manager.ServerManager;
import org.jboss.tools.teiid.reddeer.manager.VDBManager;
import org.jboss.tools.teiid.reddeer.requirement.TeiidServerRequirement;
import org.jboss.tools.teiid.reddeer.requirement.TeiidServerRequirement.TeiidServer;
import org.jboss.tools.teiid.reddeer.wizard.ImportGeneralItemWizard;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * WAR tests (REST, JBossWS-CXF) with security None | HttpBasic
 * @author lfabriko, felias
 *
 */
@RunWith(RedDeerSuite.class)
@TeiidServer(state = ServerReqState.RUNNING)
public class WARTest extends SWTBotTestCase {
	private static Logger logger = new Logger(WARTest.class);
	public static final String MODEL_PROJECT = "jdbcImportTest";
	@InjectRequirement
	private static TeiidServerRequirement teiidServer;
	private static TeiidBot teiidBot = new TeiidBot();
	private static final String oracleCP = "Oracle";
	private static final String oracleCPProps = "resources/db/oracle_books.properties";
	private static final String projectBooksWS = "BooksWS";
	private static final String vdbCheckBook="checkBookVdb";
	private static final String[] pathToCheckBookVDB = new String[]{projectBooksWS, vdbCheckBook+".vdb"};
	private static final String projectBooksRest = "BooksRest";
	private static final String vdbBooksRest = "booksRest";
	private static final String[] pathToBooksRestVDB = new String[]{projectBooksRest, vdbBooksRest+".vdb"};
	private String resultRest = "<TheBooks><Book><ISBN>0201877562</ISBN><TITLE>Software Testing in the Real World</TITLE></Book></TheBooks>";
	
	@BeforeClass
	public static void before(){
		new ShellMenu("Project", "Build Automatically").select();
		//JBossWS-CXF war
		new ImportManager().importProject("resources/projects/BooksWS");
		try{
		new WaitUntil(new ShellWithTextIsActive("Missing Password Required"), TimePeriod.NORMAL);
		new DefaultShell("Missing Password Required");
		new DefaultText(0).setText("mm");
		new PushButton("OK").click();
		}catch(Exception e){
			logger.warn("Dialog \"Missing Password Required\" haven't been showed." );
		}
		new ConnectionProfileManager().createCPWithDriverDefinition(oracleCP, oracleCPProps);
		new ModelExplorerManager().changeConnectionProfile(oracleCP, projectBooksWS, "books.xmi");
		VDBEditor ed = new VDBManager().getVDBEditor(projectBooksWS, vdbCheckBook);
		ed.synchronizeAll();
		ed.close();
		
		//RESTEasy war
		new ImportManager().importProject("resources/projects/BooksRest");
		new ModelExplorerManager().changeConnectionProfile(oracleCP, projectBooksRest, "BooksSrc.xmi");
		ed = new VDBManager().getVDBEditor(projectBooksRest, vdbBooksRest);
		ed.synchronizeAll();
		ed.close();
			
		new VDBManager().deployVDB(pathToCheckBookVDB);
		new VDBManager().createVDBDataSource(pathToCheckBookVDB);
		
		new VDBManager().deployVDB(pathToBooksRestVDB);
		new VDBManager().createVDBDataSource(pathToBooksRestVDB);
		
	}

	//@Test
	public void teiidTest(){//TODO --> move to "server - smoke tests": create source model from VDB, run DV6 and SOA5 simple mgmt tests 
			//firstly create teiid vdb
		//create source model from teiid vdb
	}
	
	/**
	 * Generate and test JBossWS-CXF WAR with security type None
	 */
	@Test
	public void jbossWSCXFNoneWarTest(){

		Properties warProps = new Properties();
		warProps.setProperty("type", "Generate SOAP War");
		warProps.setProperty("contextName", vdbCheckBook);
		warProps.setProperty("vdbJndiName", vdbCheckBook);
		warProps.setProperty("saveLocation", teiidBot.toAbsolutePath("target"));
		//http://localhost:8080/checkBookVdb/BooksInterface?wsdl
		WAR war = new VDBManager().createWAR(warProps, pathToCheckBookVDB);
		
		Properties itemProps = new Properties();
		itemProps.setProperty("dirName", teiidBot.toAbsolutePath("target"));
		itemProps.setProperty("intoFolder", projectBooksWS);
		itemProps.setProperty("file", vdbCheckBook+".war");
		new ImportManager().importGeneralItem(ImportGeneralItemWizard.Type.FILE_SYSTEM, itemProps);
		new ModelExplorerManager().getWAR(projectBooksWS, vdbCheckBook+".war").deploy();
		AbstractWait.sleep(TimePeriod.NORMAL);
		String curlNOK = "curl -u teiidUser:dvdvdv0! -H \"Content-Type: text/xml; charset=utf-8\" -H \"SOAPAction:\"  -d @"+teiidBot.toAbsolutePath("resources/wsdl/requestOracleNOK.xml")+" -X POST http://localhost:8080/checkBookVdb/BooksInterface?wsdl";
		String curlOK = "curl -u teiidUser:dvdvdv0! -H \"Content-Type: text/xml; charset=utf-8\" -H \"SOAPAction:\"  -d @"+teiidBot.toAbsolutePath("resources/wsdl/requestOracleOK.xml")+" -X POST http://localhost:8080/checkBookVdb/BooksInterface?wsdl";
		String responseNOK = teiidBot.curl(curlNOK);
		String responseOK = teiidBot.curl(curlOK);
		assertEquals(teiidBot.loadFileAsString(teiidBot.toAbsolutePath("resources/wsdl/responseOracleNOK.xml")), responseNOK);//sometimes fails even though via command line it works; curl -u testuser:testpassword -d @resources/wsdl/requestOracleNOK.xml -X POST http://localhost:8080/checkBookVdbBasic/BooksInterface?wsdl
		assertEquals(teiidBot.loadFileAsString(teiidBot.toAbsolutePath("resources/wsdl/responseOracleOK.xml")), responseOK);
	}
	
	/**
	 * Generate and test JBossWS-CXF WAR with security type Http Basic
	 */
	@Test
	public void jbossWSCXFHttpBasicWarTest(){
		String warCheckBookBasic = vdbCheckBook+"Basic";
		Properties warProps = new Properties();
		warProps.setProperty("type", WAR.JBOSSWS_CXF_TYPE);
		warProps.setProperty("contextName", warCheckBookBasic);
		warProps.setProperty("vdbJndiName", vdbCheckBook);
		warProps.setProperty("saveLocation", teiidBot.toAbsolutePath("target"));
		warProps.setProperty("securityType", WAR.HTTPBasic_SECURITY);
		warProps.setProperty("realm", "teiid-security");
		warProps.setProperty("role", "user");///this has to be set also in teiid-security-users,roles
		//http://localhost:8080/checkBookVdbBasic/BooksInterface?wsdl

		WAR war = new VDBManager().createWAR(warProps, pathToCheckBookVDB);
		
		Properties itemProps = new Properties();
		itemProps.setProperty("dirName", teiidBot.toAbsolutePath("target"));
		itemProps.setProperty("intoFolder", projectBooksWS);
		itemProps.setProperty("file", warCheckBookBasic+".war");
		new ImportManager().importGeneralItem(ImportGeneralItemWizard.Type.FILE_SYSTEM, itemProps);
		new ModelExplorerManager().getWAR(projectBooksWS, warCheckBookBasic+".war").deploy();
		AbstractWait.sleep(TimePeriod.NORMAL);
		String curlNOK = "curl -u teiidUser:dvdvdv0! -H \"Content-Type: text/xml; charset=utf-8\" -H \"SOAPAction:\"  -d @"+teiidBot.toAbsolutePath("resources/wsdl/requestOracleNOK.xml")+" -X POST http://localhost:8080/checkBookVdbBasic/BooksInterface?wsdl";
		String curlOK =  "curl -u teiidUser:dvdvdv0! -H \"Content-Type: text/xml; charset=utf-8\" -H \"SOAPAction:\"  -d @"+teiidBot.toAbsolutePath("resources/wsdl/requestOracleOK.xml")+" -X POST http://localhost:8080/checkBookVdbBasic/BooksInterface?wsdl";
		String responseNOK = teiidBot.curl(curlNOK);
		String responseOK = teiidBot.curl(curlOK);
		assertEquals(teiidBot.loadFileAsString(teiidBot.toAbsolutePath("resources/wsdl/responseOracleNOK.xml")), responseNOK);
		assertEquals(teiidBot.loadFileAsString(teiidBot.toAbsolutePath("resources/wsdl/responseOracleOK.xml")), responseOK);
	}
	
	/**
	 * Create RESTEasy WAR with security type None
	 */
	@Test
	public void restWarNoneTest(){
	
		String rbWar = "restBooks";

		Properties warProps = new Properties();
		warProps.setProperty("type", WAR.RESTEASY_TYPE);
		warProps.setProperty("contextName", rbWar);
		warProps.setProperty("vdbJndiName", vdbBooksRest);
		warProps.setProperty("saveLocation", teiidBot.toAbsolutePath("target"));
		warProps.setProperty("securityType", WAR.NONE_SECURITY);
		new VDBManager().createWAR(warProps, pathToBooksRestVDB);
	
		//import created war
		Properties itemProps = new Properties();
		itemProps.setProperty("dirName", teiidBot.toAbsolutePath("target"));
		itemProps.setProperty("file", rbWar + ".war");
		itemProps.setProperty("intoFolder", projectBooksRest);


		new ImportManager().importGeneralItem(ImportGeneralItemWizard.Type.FILE_SYSTEM, itemProps);

		
		new ModelExplorerManager().getWAR(projectBooksRest, rbWar+".war").deploy();
		AbstractWait.sleep(TimePeriod.NORMAL);
		String url = "http://localhost:8080/"+rbWar+"/BooksView/book1/0201877562";
		
		assertEquals(resultRest, teiidBot.curl(url));
		
	}
	
	/**
	 * Create RESTEasy WAR with security type Http Basic
	 */
	@Test
	public void restWarBasicTest(){

		String rbWar = "restBooksBasic";

		Properties warProps = new Properties();
		warProps.setProperty("type", WAR.RESTEASY_TYPE);
		warProps.setProperty("contextName", rbWar);
		warProps.setProperty("vdbJndiName", vdbBooksRest);
		warProps.setProperty("saveLocation", teiidBot.toAbsolutePath("target"));
		warProps.setProperty("securityType", WAR.HTTPBasic_SECURITY);
		warProps.setProperty("realm", "teiid-security");
		warProps.setProperty("role", "user");

		new VDBManager().createWAR(warProps, pathToBooksRestVDB);

		//import created war
		Properties itemProps = new Properties();
		itemProps.setProperty("dirName", teiidBot.toAbsolutePath("target"));
		itemProps.setProperty("file", rbWar + ".war");
		itemProps.setProperty("intoFolder", projectBooksRest);
		new ImportManager().importGeneralItem(ImportGeneralItemWizard.Type.FILE_SYSTEM, itemProps);
	
		
		new ModelExplorerManager().getWAR(projectBooksRest, rbWar+".war").deploy();
		
		String url = "-u teiidUser:dvdvdv0! http://localhost:8080/"+rbWar+"/BooksView/book1/0201877562";
		assertEquals(resultRest, teiidBot.curl(url));
	}

}

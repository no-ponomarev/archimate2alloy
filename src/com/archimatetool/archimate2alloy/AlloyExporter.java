/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.archimate2alloy;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.diagram.actions.Messages;
import com.archimatetool.editor.diagram.commands.FillColorCommand;
import com.archimatetool.editor.model.IModelExporter;
import com.archimatetool.editor.model.commands.EObjectFeatureCommand;
import com.archimatetool.editor.preferences.IPreferenceConstants;
import com.archimatetool.editor.preferences.Preferences;
import com.archimatetool.editor.ui.ColorFactory;
import com.archimatetool.editor.ui.factory.IObjectUIProvider;
import com.archimatetool.editor.ui.factory.ObjectUIFactory;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.ILockable;
import com.sun.prism.paint.Color;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4.SafeList;
import edu.mit.csail.sdg.alloy4compiler.ast.Module;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Command;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.parser.CompUtil;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Options;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4compiler.translator.TranslateAlloyToKodkod;
import edu.mit.csail.sdg.alloy4viz.VizGUI;


/**
 * Example Exporter of Archimate model to Alloy
 * 
 */
public class AlloyExporter implements IModelExporter {
    
	private IArchimateModel archimateModel;
	private File alloyFile;
    private OutputStreamWriter writer;
    private List<CustomSig> sigList;
    
    private final static Map<String, String> relationsTransform;
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("AggregationRelationship", "aggregates");
        aMap.put("ServingRelationship", "uses");
        aMap.put("AssignmentRelationship", "assigned_to");
        aMap.put("TriggeringRelationship", "triggers");
        aMap.put("RealizationRelationship", "realizes");
        aMap.put("AccessRelationship", "accesses");
        aMap.put("CompositionRelationship", "composes");
        aMap.put("FlowRelationship", "flow_to");
        aMap.put("AssociationRelationship", "associated_with");
        relationsTransform = Collections.unmodifiableMap(aMap);
    };
    
    private class CustomSig {
    	public String name;
    	public String underscoreName;
    	public String parentName;
    	public Map<String, String> relationsMap;
    	public IDiagramModelArchimateObject diagramObject;
    	
    	public CustomSig(IDiagramModelArchimateObject diagramObject, String name, String parentName) {
    		this.diagramObject = diagramObject;
    		this.name = name;
    		this.parentName = parentName;
    		this.relationsMap = new HashMap<String, String>();
    		this.underscoreName = name.replaceAll(" ", "_").replaceAll("-", "_").toLowerCase();
    	}
    	
    	public void addRelation(String type, String target) {
    		String key = relationsTransform.get(type);
    		if (relationsMap.containsKey(key)) {
    			relationsMap.replace(key, relationsMap.get(key) + " + " + target);
    		} else {
    			relationsMap.put(relationsTransform.get(type), target);
    		}
    	}
    	
    	@Override
    	public String toString() {
			StringBuilder result = new StringBuilder();
			result.append("sig " + underscoreName + " extends " + parentName + " {}{\n");
			
			List<String> noneSigs = AlloySigs.archimateSigs.get(parentName);
			for (String sig : noneSigs) {
				if (!relationsMap.containsKey(sig)) {
					relationsMap.put(sig, "none");
				}
			}
			
			for (Map.Entry<String, String> entry : relationsMap.entrySet()) {
			    String key = entry.getKey();
			    String value = entry.getValue();
			    result.append("    " + key + " = " + value + "\n");
			}
			
			result.append("}\n");
    		return result.toString();	
    	}
    }
    
    // Default constructor
    public AlloyExporter() {}
    
    @Override
    public void export(IArchimateModel model) throws IOException {
    	archimateModel = model;
    	alloyFile = new File("C:\\Users\\nponomar\\Google Drive\\Education\\Masters.Diploma\\AlloyFiles\\SimpleInheritance\\CaseExample.als");
    	writer = new OutputStreamWriter(new FileOutputStream(alloyFile));
    	sigList = new ArrayList<CustomSig>();
    	
    	// Schedule a job for the event dispatch thread
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	// Create and set up the window
            	AlloyQuestionInterface frame = new AlloyQuestionInterface("Ask question to the model");
                frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                frame.addComponentsToPane(frame.getContentPane());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
        
    }

    //-------------------------------------------------------------------------------
    // Alloy model from Archimate
    //-------------------------------------------------------------------------------
    
    private void transformModel(IArchimateModel model) throws IOException, Err {
    	List<EObject> relationsList = new ArrayList<EObject>();
    	getElements(model.getFolder(FolderType.RELATIONS), relationsList);
    	
    	List<EObject> elementsList = new ArrayList<EObject>();
    	getElements(model.getFolder(FolderType.BUSINESS), elementsList);
    	getElements(model.getFolder(FolderType.APPLICATION), elementsList);
    	getElements(model.getFolder(FolderType.TECHNOLOGY), elementsList);
    	getElements(model.getFolder(FolderType.MOTIVATION), elementsList);
    	
    	transformLayers(elementsList, sigList);
    	transformRelations(relationsList, sigList);
    	writeToAlloyFile(sigList);
    }
    
    private void transformLayers(List<EObject> elementsList, List<CustomSig> sigList) throws IOException, Err {
        for(EObject eObject : elementsList) {
        	IArchimateElement component = (IArchimateElement) eObject;
        	List<IDiagramModelArchimateObject> object = component.getReferencingDiagramObjects();
        	
        	CustomSig componentSig = new CustomSig(object.get(0), component.getName(), component.eClass().getName());
            sigList.add(componentSig);
        }
    }
    
    private void transformRelations(List<EObject> relationsList, List<CustomSig> sigList) throws IOException, Err {
        for(EObject eObject : relationsList) {
        	IArchimateRelationship relationElement = (IArchimateRelationship) eObject;
        	IArchimateElement sourceElement = (IArchimateElement) relationElement.getSource();
        	IArchimateElement targetElement = (IArchimateElement) relationElement.getTarget();
        	
        	List<CustomSig> sourceResult = sigList.stream()
        			.filter(item -> item.name.equals(sourceElement.getName()))
        			.collect(Collectors.toList());
        	
        	List<CustomSig> targetResult = sigList.stream()
        			.filter(item -> item.name.equals(targetElement.getName()))
          		    .collect(Collectors.toList());
        
        	CustomSig sourceSig = sourceResult.get(0);
        	CustomSig targetSig = targetResult.get(0);
        	
            sourceSig.addRelation(relationElement.getClass().getSimpleName(), targetSig.underscoreName);
        }
    }
    
    private void writeToAlloyFile(List<CustomSig> sigList) throws IOException {
    	for (CustomSig sig : sigList) {
    		writer.write(sig.toString() + "\n");
    	}
    }
    
    private void getElements(IFolder folder, List<EObject> list) {
        for(EObject object : folder.getElements()) { list.add(object); }
        for(IFolder f : folder.getFolders()) { getElements(f, list); }
    }
    
    private void createAlloyModelFromAchimate(IArchimateModel model) throws Err, IOException {
    	writer.write("open ArchimateMetaModel as ArchiCore\n\n");
    	
    	// Transform all elements to Sigs and Relations
    	transformModel(model);
    }
    
    //-------------------------------------------------------------------------------
    // Ask question to Alloy module
    //-------------------------------------------------------------------------------
    
    private String askQuestion() {
    	JTextArea questionField = new JTextArea(10, 20);
    	JScrollPane scrollPane = new JScrollPane(questionField);  
    	questionField.setLineWrap(true);
    	questionField.setWrapStyleWord(true);
    	
    	JOptionPane.showMessageDialog(null, scrollPane, "Ask question to the model: ", JOptionPane.QUESTION_MESSAGE);
    	
    	return questionField.getText();
    }
    
    //-------------------------------------------------------------------------------
    // Append assertion asked by user to alloy file
    //-------------------------------------------------------------------------------
    
    private void appendAlloyQuestion(String alloyQuestion, String scopeChoice) throws IOException  {
//    	writer.write("fact {\n");
//    	for (CustomSig sig : sigList) {
//    		writer.write("#" + sig.underscoreName + " = 1\n");
//    	}
//    	writer.write("}\n");
    	
    	//writer.write("pred show {}\nrun show for 1");
    	
    	StringBuilder scope = new StringBuilder();
    	if ( scopeChoice.equals("full")) {
    		scope.append(" but ");
        	String prefix = "";
        	for (CustomSig sig : sigList) {
        		scope.append(prefix + " exactly 1 " + sig.underscoreName);
        		prefix = ",";
        	}
    	}
    	
    	writer.write(alloyQuestion);
    	writer.write(scope.toString());
    	writer.close();
    }
    
    //-------------------------------------------------------------------------------
    // Validate Alloy model with user assert
    //-------------------------------------------------------------------------------
    
    private void validateModel() throws Err {        
        // Alloy4 sends diagnostic messages and progress reports to the A4Reporter.
        // By default, the A4Reporter ignores all these events (but you can extend the A4Reporter to display the event for the user)
        A4Reporter reporter = new A4Reporter() {
            // For example, here we choose to display each "warning" by printing it to System.out
            @Override public void warning(ErrorWarning msg) {
                System.out.print("Relevance Warning:\n" + (msg.toString().trim()) + "\n\n");
                System.out.flush();
            }
        };
        
        // Parse + Type Check the model
        System.out.println("=========== Parsing + Typechecking " + alloyFile.getName() + " =============");
        Module world = CompUtil.parseEverything_fromFile(reporter, null, alloyFile.getAbsolutePath());

        // Choose some default options for how you want to execute the commands
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;
        
        // Retrieve only first command, which should be an assertion
        Command command = world.getAllCommands().get(0);

        // Execute the command
        System.out.println("============ Command " + command + ": ============");
        A4Solution solution = TranslateAlloyToKodkod.execute_command(reporter, world.getAllReachableSigs(), command, options);  
        
        parseSolution(command, solution);
    }
    
    //-------------------------------------------------------------------------------
    // Show results somehow
    //-------------------------------------------------------------------------------
    
    private void parseSolution(Command command, A4Solution solution) throws Err {
        if (solution.satisfiable()) {
        	
        	// Show Viz GUI
        	// VizGUI viz = new VizGUI(false, xmlResult, null);
        	
        	// Retrieve all skolems, that is appeared in solution
        	Set<String> solutionSigSet = new LinkedHashSet<String>();
        	for (ExprVar skolem : solution.getAllSkolems()) {
            	String expressionSig = solution.eval(skolem).toString();
            	expressionSig = expressionSig.substring(expressionSig.indexOf('{') + 1, expressionSig.indexOf('$'));
            	solutionSigSet.add(expressionSig);
        	}
        	
        	if (solutionSigSet.isEmpty()) {
	        	// Retrieve all atoms, that is appeared in solution
	            for (ExprVar atom: solution.getAllAtoms()) {
	            	String[] sigParts = atom.label.split("\\$");
	            	solutionSigSet.add(sigParts[0]);
	            }
        	}

            for (String sigLabel : solutionSigSet) {
            	List<CustomSig> foundSig = sigList.stream()
            			.filter(item -> item.underscoreName.equals(sigLabel))
            			.collect(Collectors.toList());
            	
            	if (!foundSig.isEmpty()) {
            		CustomSig solutionSig = foundSig.get(0);
            		
            		Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                        	
                        	RGB newColor = null;
                        	if (command.check) {
                        		newColor = new RGB(255,0,0);
                        	} else {
                        		newColor = new RGB(0,255,0);
                        	}
                        		
                        	org.eclipse.gef.commands.Command cmd = new FillColorCommand((IDiagramModelObject)solutionSig.diagramObject, ColorFactory.convertRGBToString(newColor));
                            CommandStack stack = (CommandStack) archimateModel.getAdapter(CommandStack.class);
                            stack.execute(cmd);
                        }
                     });
            		
            	}
            }

        } else {
        	JOptionPane.showMessageDialog(null, "No example/counter-example found!");
        }
    }
    
    private class AlloyQuestionInterface extends JFrame {
    	JPanel textFieldPanel;
    	JPanel optionsPanel;
    	JPanel applyPanel;
    	
    	JTextArea questionField;
    	
    	ButtonGroup radioGroup;
    	JRadioButton fullScope;
    	JRadioButton customScope;
    	
        public AlloyQuestionInterface(String name) {
            super(name);
            
        }

        public void addComponentsToPane(final Container pane) {
            textFieldPanel = new JPanel();
            textFieldPanel.setLayout(new GridLayout(0,1));
            
            optionsPanel = new JPanel();
            optionsPanel.setLayout(new GridLayout(1,0));
            
            applyPanel = new JPanel();
            applyPanel.setLayout(new GridLayout(0,1));
             
            
            questionField = new JTextArea(10, 40);
        	JScrollPane scrollPane = new JScrollPane(questionField);  
        	questionField.setLineWrap(true);
        	questionField.setWrapStyleWord(true);
             
            textFieldPanel.add(scrollPane);
            
            fullScope = new JRadioButton("Use full scope");
            fullScope.setActionCommand("full");
            fullScope.setSelected(true);
     
            customScope = new JRadioButton("Custom scope");
            customScope.setActionCommand("custom");
            
            radioGroup = new ButtonGroup();
            radioGroup.add(fullScope);
            radioGroup.add(customScope); 
            
            optionsPanel.add(fullScope);
            optionsPanel.add(customScope);
            
            JButton applyButton = new JButton("Find solution");
            applyPanel.add(applyButton);
             
            //Process the Apply gaps button press
            applyButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                	
                	try {
            	    	// Create Alloy model file from Archimate
            			createAlloyModelFromAchimate(archimateModel);
            			
            	        // Append question to Alloy file
            	        appendAlloyQuestion(questionField.getText(), radioGroup.getSelection().getActionCommand());
            	        
            	        // Parse and validate Alloy model
            	        validateModel();
            	        
            		} catch (Err e1) {
            			JOptionPane.showMessageDialog(null, "No example/counterexample found!");
            			e1.printStackTrace();
            		} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
                	
                	// Close frame
        	        dispose();             
                }
            });
            
            pane.add(textFieldPanel, BorderLayout.NORTH);
            pane.add(optionsPanel, BorderLayout.CENTER);
            pane.add(applyPanel, BorderLayout.SOUTH);
        }
    }
    
}

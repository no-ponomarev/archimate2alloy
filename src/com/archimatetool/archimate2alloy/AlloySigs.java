package com.archimatetool.archimate2alloy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlloySigs {
	
	public final static Map<String, List<String>> archimateSigs;
    static {
        Map<String, List<String>> aMap = new HashMap<String, List<String>>();
     
        aMap.put("BusinessInterface",  new ArrayList<String>(Arrays.asList("assigned_to", "composes")));

        aMap.put("ApplicationInterface", new ArrayList<String>(Arrays.asList("assigned_to", "composes")));
        
        aMap.put("TechnologyInterface", new ArrayList<String>(Arrays.asList("assigned_to")));

        aMap.put("BusinessRole", new ArrayList<String>(Arrays.asList("assigned_to", "uses")));

        aMap.put("BusinessActor", new ArrayList<String>(Arrays.asList("assigned_to", "uses")));

        aMap.put("Location", new ArrayList<String>(Arrays.asList("assigned_to")));

        aMap.put("ApplicationComponent", new ArrayList<String>(Arrays.asList("uses", "assigned_to", "realizes")));

        aMap.put("Node", new ArrayList<String>(Arrays.asList("uses", "assigned_to", "realizes", "aggregates")));

        aMap.put("CommunicationPath", new ArrayList<String>(Arrays.asList("assosiated_with")));

        aMap.put("Device", new ArrayList<String>(Arrays.asList("uses", "assigned_to", "realizes", "associated_with")));

        aMap.put("CommunicationNetwork", new ArrayList<String>(Arrays.asList("realizes", "associated_with")));

        aMap.put("SystemSoftware", new ArrayList<String>(Arrays.asList()));

        aMap.put("BusinessService", new ArrayList<String>(Arrays.asList("accesses", "uses")));

        aMap.put("ApplicationService", new ArrayList<String>(Arrays.asList("accesses", "uses")));

        aMap.put("TechnologyService", new ArrayList<String>(Arrays.asList("accesses", "uses")));

        aMap.put("BusinessProcess", new ArrayList<String>(Arrays.asList("realizes", "uses", "flows_to", "accesses", "triggers", "aggregates")));

        aMap.put("BusinessFunction", new ArrayList<String>(Arrays.asList("realizes", "uses", "flows_to", "accesses", "triggers")));

        aMap.put("BusinessEvent", new ArrayList<String>(Arrays.asList("accesses", "triggers")));

        aMap.put("ApplicationFunction", new ArrayList<String>(Arrays.asList("uses", "realizes", "triggers", "flow_to", "accesses")));

        aMap.put("ApplicationInteraction", new ArrayList<String>(Arrays.asList("assigned_to", "composes")));

        aMap.put("TechnologyFunction", new ArrayList<String>(Arrays.asList("uses", "realizes", "triggers", "flow_to", "accesses")));

        aMap.put("BusinessObject", new ArrayList<String>(Arrays.asList("associated_with")));

        aMap.put("Product", new ArrayList<String>(Arrays.asList("assigned_to", "aggregates", "uses")));

        aMap.put("Contract", new ArrayList<String>(Arrays.asList()));

        aMap.put("Meaning", new ArrayList<String>(Arrays.asList()));

        aMap.put("Value", new ArrayList<String>(Arrays.asList()));

        aMap.put("DataObject", new ArrayList<String>(Arrays.asList()));

        aMap.put("Artifact", new ArrayList<String>(Arrays.asList("realizes")));

        archimateSigs = Collections.unmodifiableMap(aMap);
    };
	
}

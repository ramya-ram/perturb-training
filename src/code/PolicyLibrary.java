package code;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a list of policies that make up the library used in policy reuse
 */
public class PolicyLibrary {
	public List<Policy> policyLibrary;

	public PolicyLibrary(List<Policy> policyLibrary) {
		this.policyLibrary = policyLibrary;
	}
	
	public PolicyLibrary() {
		policyLibrary = new ArrayList<Policy>();
	}
	
	public List<Policy> getPolicyLibrary() {
		return policyLibrary;
	}
	
	public int size(){
		return policyLibrary.size();
	}
	
	public Policy get(int index){
		return policyLibrary.get(index);
	}
	
	public void add(Policy policy){
		policyLibrary.add(policy);
	}
	
	public void addAll(List<Policy> policies){
		policyLibrary.addAll(policies);
	}
	
	public void remove(Policy policy){
		policyLibrary.remove(policy);
	}
	
	public void printWeights(){
		for(Policy policy : policyLibrary)
			System.out.print(policy.weight+" ");
		System.out.println();
	}
	
	public void printNumEpisodesChosen(){
		for(Policy policy : policyLibrary)
			System.out.print(policy.numEpisodesChosen+" ");
		System.out.println();
	}
}

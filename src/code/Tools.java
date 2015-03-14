package code;

import java.util.Random;
import java.util.Scanner;

/**
 * A Tools class that contains common methods
 */
public class Tools {
	public static Random rand = new Random(); 
	public static Scanner scan = new Scanner(System.in);
	
	/**
	 * Calculate index of max value in array
	 */
	public static int calculateMax(double[] arr){
		double maxValue = Integer.MIN_VALUE;
		int maxIndex = -1;
		for(int i=0; i<arr.length; i++){
			System.out.println(""+arr[i]);
			if(arr[i] > maxValue){
				maxValue = arr[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
	
	public static void printArray(Object[] arr){
		for(int i=0; i<arr.length; i++)
			System.out.print(arr[i]+", ");
		System.out.println();
	}
	
	/*public static void printArray(int[] arr){
		for(int i=0; i<arr.length; i++)
			System.out.print(arr[i]+", ");
		System.out.println();
	}*/
}

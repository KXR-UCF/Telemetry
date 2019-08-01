import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DatasetsController {

	private static Map<Integer, Dataset> datasets = new TreeMap<Integer, Dataset>();
	private static AtomicInteger sampleCount = new AtomicInteger(0);
	private static List<Consumer<Boolean>> sampleCountListeners = new ArrayList<Consumer<Boolean>>(); // true = (sampleCount >= 1)
	
	// timestamps are stored in an array of long[]'s, each containing 1M longs, and allocated as needed.
	private static final int slotSize = (int) Math.pow(2, 20); // 1M longs per slot
	private static final int slotCount = (Integer.MAX_VALUE / slotSize) + 1;
	private static long[][] timestamps = new long[slotCount][];
	
	/**
	 * Registers a listener that will be notified when the sample count is set to 1 or 0, and triggers an event to ensure the GUI is in sync.
	 * 
	 * @param listener    The listener to be notified.
	 */
	public static void addSampleCountListener(Consumer<Boolean> listener) {
		
		sampleCountListeners.add(listener);
		notifySampleCountListeners();
		
	}
	
	/**
	 * Notifies all registered listeners about the sample count.
	 */
	private static void notifySampleCountListeners() {
		
		for(Consumer<Boolean> listener : sampleCountListeners)
			listener.accept(sampleCount.get() >= 1);
		
	}
	
	/**
	 * @return    The number of fields in the data structure.
	 */
	public static int getDatasetsCount() {
		
		return datasets.size();
		
	}
	
	/**
	 * @param index    An index between 0 and getDatasetsCount()-1, inclusive.
	 * @return         The Dataset.
	 */
	public static Dataset getDatasetByIndex(int index) {
		
		return (Dataset) datasets.values().toArray()[index];
		
	}
	
	/**
	 * @param location    CSV column number, or Binary packet byte offset. Locations may be sparse.
	 * @return            The Dataset, or null if it does not exist.
	 */
	public static Dataset getDatasetByLocation(int location) {
		
		return datasets.get(location);

	}
	
	/**
	 * Creates and stores a new Dataset. If a Dataset already exists for the same location, the new Dataset will replace it.
	 * 
	 * @param location             CSV column number, or Binary packet byte offset.
	 * @param processor            BinaryProcessor for the raw samples in the Binary packet. (Ignored in CSV mode, use null.)
	 * @param name                 Descriptive name of what the samples represent.
	 * @param color                Color to use when visualizing the samples.
	 * @param unit                 Descriptive name of how the samples are quantified.
	 * @param conversionFactorA    This many unprocessed LSBs...
	 * @param conversionFactorB    ... equals this many units.
	 */
	public static void insertDataset(int location, BinaryFieldProcessor processor, String name, Color color, String unit, float conversionFactorA, float conversionFactorB) {
		
		datasets.put(location, new Dataset(location, processor, name, color, unit, conversionFactorA, conversionFactorB));
		
	}
	
	/**
	 * Removes a specific dataset.
	 * 
	 * @return    true on success, false if nothing existed there.
	 */
	public static boolean removeDataset(int location) {
		
		PositionedChart[] charts = Controller.getCharts().toArray(new PositionedChart[0]);
		for(PositionedChart chart : charts)
			for(Dataset dataset : chart.datasets)
				if(dataset.location == location)
					Controller.removeChart(chart);
		
		Dataset removedDataset = datasets.remove(location);
		
		if(datasets.isEmpty()) {
			sampleCount.set(0);
			notifySampleCountListeners();
		}
		
		if(removedDataset == null)
			return false;
		else
			return true;
		
	}
	
	/**
	 * Removes all charts and Datasets.
	 */
	public static void removeAllDatasets() {
		
		Controller.removeAllCharts();
		
		datasets.clear();
		sampleCount.set(0);
		notifySampleCountListeners();
		
	}
	
	/**
	 * @return    The Datasets.
	 */
	public static Dataset[] getAllDatasets() {
		
		return datasets.values().toArray(new Dataset[datasets.size()]);
		
	}
	
	/**
	 * Increments the sample count. Call this function after all datasets have received a new value.
	 */
	public static void incrementSampleCount() {
		
		int currentSize = getSampleCount();
		int slotNumber = currentSize / slotSize;
		int slotIndex  = currentSize % slotSize;
		if(slotIndex == 0)
			timestamps[slotNumber] = new long[slotSize];
		timestamps[slotNumber][slotIndex] = System.currentTimeMillis();
		
		int newSampleCount = sampleCount.incrementAndGet();
		if(newSampleCount == 1)
			notifySampleCountListeners();
		
	}
	
	/**
	 * Increments the sample count and sets the timestamp to a specific value. Call this function when importing a file, after all datasets have received a new value.
	 */
	public static void incrementSampleCountWithTimestamp(long timestamp) {
		
		int currentSize = getSampleCount();
		int slotNumber = currentSize / slotSize;
		int slotIndex  = currentSize % slotSize;
		if(slotIndex == 0)
			timestamps[slotNumber] = new long[slotSize];
		timestamps[slotNumber][slotIndex] = timestamp;
		
		int newSampleCount = sampleCount.incrementAndGet();
		if(newSampleCount == 1)
			notifySampleCountListeners();
		
	}
	
	/**
	 * Gets the timestamp for one specific sample.
	 * 
	 * @param sampleNumber    Which sample to check.
	 * @return                The corresponding UNIX timestamp.
	 */
	public static long getTimestamp(int sampleNumber) {
		
		int slotNumber = sampleNumber / slotSize;
		int slotIndex  = sampleNumber % slotSize;
		return timestamps[slotNumber][slotIndex];
		
	}
	
	/**
	 * @return    The current number of samples stored in the Datasets.
	 */
	public static int getSampleCount() {
		
		return sampleCount.get();
		
	}
	
}

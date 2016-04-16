package org.jboss.seam.deployment;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

public class ScanResultsCache {

	public static final String KEY_CACHE_SCAN_RESULTS = "org.jboss.seam.deployment.CACHE_SCAN_RESULTS";
	private static ScanResultsCache instance = new ScanResultsCache();
	
	public static ScanResultsCache getInstance() {
		return instance;
	}
	public static ScanResultsCache getInstance(ServletContext context) {
		
		if (context.getAttribute(KEY_CACHE_SCAN_RESULTS) == null) {
			Boolean useCache = Boolean.parseBoolean(context.getInitParameter(KEY_CACHE_SCAN_RESULTS));
			ScanResultsCache cache = null;
			if (useCache) {
				cache = instance;
			}
			else {
				cache = new ScanResultsCache(){
					public void addHit(String hit) {};
					public void addMiss(String hit) {};
				};
			}
			context.setAttribute(KEY_CACHE_SCAN_RESULTS, cache);				
			
			return cache;
			
		}
		
		return (ScanResultsCache) context.getAttribute(KEY_CACHE_SCAN_RESULTS);		
	}
	
	
	private Set<String> hits = new HashSet<String>();
	private Set<String> misses = new HashSet<String>();
	
	private ScanResultsCache() {
		super();
	}
	
	public void addHit (String hit) {
		this.hits.add(hit);
	}
	public void addMiss(String miss) {
		this.misses.add(miss);
	}
	
	public boolean isHit(String test){
		return this.hits.contains(test);
	}
	public boolean isMiss(String test) {
		return this.misses.contains(test);
	}
	public boolean isIndeterminate(String test) {
		return !isHit(test) && !isMiss(test);
	}
	
	public void reset() {
		this.hits.clear();
		this.misses.clear();
	}

}

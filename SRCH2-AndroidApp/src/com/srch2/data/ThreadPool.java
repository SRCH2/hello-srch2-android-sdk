/*
 * Copyright (c) 2016, SRCH2
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of the SRCH2 nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL SRCH2 BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.srch2.data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.srch2.Pith;

import android.util.Log;

public class ThreadPool {
	private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
	private static int KEEP_ALIVE_TIME = 500;
	
	private BlockingQueue<Runnable> taskQueue;
	private BlockingQueue<Runnable> searchTaskQueue;
	private BlockingQueue<Runnable> updateTaskQueue;
	private BlockingQueue<Runnable> saveAndDisposeTaskQueue;


	private ExecutorService createAndLoadExecutor;
	private ExecutorService searchExecutor;
	private ExecutorService updateExecutor;
	private ExecutorService saveAndDisposeExecutor;
	
	
	public static boolean isSingleCore() { return NUMBER_OF_CORES == 1; }
	
	public static enum ThreadTaskType {
		Open,
		Search,
		Update,
		Close
	}
	
	private volatile boolean isAvailable;
	
	public ThreadPool() {
		isAvailable = true;
		int creationProcessThreadPoolSize = Math.max(1, NUMBER_OF_CORES);
		taskQueue = new LinkedBlockingQueue<Runnable>();
		createAndLoadExecutor = new ThreadPoolExecutor(creationProcessThreadPoolSize, creationProcessThreadPoolSize, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, taskQueue);
		
		searchTaskQueue = new LinkedBlockingQueue<Runnable>();
		searchExecutor = new ThreadPoolExecutor(creationProcessThreadPoolSize, creationProcessThreadPoolSize, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, searchTaskQueue);
		
		saveAndDisposeTaskQueue = new LinkedBlockingQueue<Runnable>();
		saveAndDisposeExecutor = new ThreadPoolExecutor(creationProcessThreadPoolSize, creationProcessThreadPoolSize, 100, TimeUnit.MILLISECONDS, saveAndDisposeTaskQueue);
	
		updateTaskQueue = new LinkedBlockingQueue<Runnable>();
		updateExecutor = new ThreadPoolExecutor(1, 2, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, updateTaskQueue);
	}
	
	public void doRunnableTask(Runnable r, ThreadTaskType type) {

	
		switch (type) {
			case Open:
				try {
					if (isAvailable) {
						createAndLoadExecutor.execute(r);
					}
				} catch (RejectedExecutionException ree) {
					Pith.handleException(ree);
				}
				
				break;
			case Search:
				try {
					if (searchExecutor != null && !searchExecutor.isShutdown() && isAvailable) {
						searchExecutor.execute(r);
					}
				} catch (RejectedExecutionException ree) {
					Pith.handleException(ree);
				}
				
				break;
			case Close:
				if (saveAndDisposeExecutor != null && !saveAndDisposeExecutor.isShutdown()) {
					try {
						saveAndDisposeExecutor.execute(r);		
					} catch (RejectedExecutionException ree) {
						Pith.handleException(ree);
					}
				}

				break;
			case Update:
				try {
					if (updateExecutor != null && !updateExecutor.isShutdown() && isAvailable) {
						updateExecutor.execute(r);
					}
				} catch (RejectedExecutionException ree) {
					Pith.handleException(ree);
				}
				break;
		}

	}
	

	
	public void wakeUp() {

		isAvailable = true;
		int creationProcessThreadPoolSize = Math.max(1, NUMBER_OF_CORES);
		if (createAndLoadExecutor != null && createAndLoadExecutor.isShutdown()) {
			taskQueue = null;
			createAndLoadExecutor = null;
			
			taskQueue = new LinkedBlockingQueue<Runnable>();
			createAndLoadExecutor = new ThreadPoolExecutor(creationProcessThreadPoolSize, creationProcessThreadPoolSize, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, taskQueue);
		}
		

		if (searchExecutor != null && searchExecutor.isShutdown()) {
			searchTaskQueue = null;
			searchExecutor = null;
			
			searchTaskQueue = new LinkedBlockingQueue<Runnable>();
			searchExecutor = new ThreadPoolExecutor(creationProcessThreadPoolSize, creationProcessThreadPoolSize, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, searchTaskQueue);
		}
		
		if (updateExecutor != null && updateExecutor.isShutdown()) {
			updateTaskQueue = null;
			updateExecutor = null;
			
			updateTaskQueue = new LinkedBlockingQueue<Runnable>();
			updateExecutor = new ThreadPoolExecutor(1, 2, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, updateTaskQueue);
		}
		

		if (saveAndDisposeExecutor != null && saveAndDisposeExecutor.isShutdown()) {
			saveAndDisposeTaskQueue = null;
			saveAndDisposeExecutor = null;
			
			saveAndDisposeTaskQueue = new LinkedBlockingQueue<Runnable>();
			saveAndDisposeExecutor = new ThreadPoolExecutor(creationProcessThreadPoolSize, creationProcessThreadPoolSize, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, saveAndDisposeTaskQueue);
		}
	}
	
	public void onPause() {
		isAvailable = false;
		if (searchExecutor != null && !searchExecutor.isShutdown()) {
			searchExecutor.shutdownNow();	
		}
		
		if (updateExecutor != null && !updateExecutor.isShutdown()) {
			updateExecutor.shutdownNow();
		}

		if (createAndLoadExecutor != null && !createAndLoadExecutor.isShutdown()) {
			createAndLoadExecutor.shutdown();
		}
	}
	
	public void onDestroy() {
		if (createAndLoadExecutor != null) {
			createAndLoadExecutor.shutdownNow();
		}
		

		if (saveAndDisposeExecutor != null) {
			saveAndDisposeExecutor.shutdown();
		}
	}
}

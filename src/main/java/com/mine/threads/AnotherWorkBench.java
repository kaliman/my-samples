package com.mine.threads;

import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnotherWorkBench {

	private static final CountDownLatch latch = new CountDownLatch(4);
	static final List<Long> threadIds = Collections.synchronizedList(new ArrayList<Long>());

	private void dummyJob() {
		List<Long> primes = new ArrayList<Long>();
		long i = 0;
		while (true) {
			primes.add(++i);
			if ((i % 10) == 0) {
				primes.clear();
				//introduce sleep to prevent process hogging
				try {
					Thread.currentThread().sleep(2000);
				} catch (InterruptedException ex) {
					Logger.getLogger(AnotherWorkBench.class.getName()).log(Level.SEVERE, null, ex);
				}
				System.runFinalization();
				System.gc();
			}
		}
	}

	private void runDummyJobs() {

		Runnable dummyJob = new Runnable() {
			@Override
			public void run() {
				threadIds.add(Thread.currentThread().getId());
				latch.countDown();
				dummyJob();
			}
		};

		Runnable memoryMonitorJob = new Runnable() {
			@Override
			public void run() {

				System.out.println(Thread.currentThread().getName() + " : Monitor thread started");
				ThreadMXBean threadMxBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
				threadMxBean.setThreadAllocatedMemoryEnabled(true);

				while (true) {
					for (Long threadId : threadIds) {
						System.out.println(Thread.currentThread().getName() + " : Thread ID : " + threadId + " : memory = " + threadMxBean.getThreadAllocatedBytes(threadId) + " bytes");
					}

					//wait between subsequent scans
					try {
						System.out.println("================================");
						Thread.currentThread().sleep(5000);
						//System.out.println(Thread.currentThread().getName() + " : out of secondary sleep");
					} catch (InterruptedException ex) {
						Logger.getLogger(AnotherWorkBench.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		};

		Executors.newSingleThreadExecutor().submit(dummyJob);
		Executors.newSingleThreadExecutor().submit(dummyJob);
		Executors.newSingleThreadExecutor().submit(dummyJob);
		Executors.newSingleThreadExecutor().submit(dummyJob);

		try {
			latch.await();
		} catch (InterruptedException ex) {
			Logger.getLogger(AnotherWorkBench.class.getName()).log(Level.SEVERE, null, ex);
		}
		Executors.newSingleThreadExecutor().submit(memoryMonitorJob);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new AnotherWorkBench().runDummyJobs();
	}
}

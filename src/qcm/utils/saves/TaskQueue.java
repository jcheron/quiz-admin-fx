package qcm.utils.saves;

import java.util.Observable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import qcm.utils.WebGate;

public class TaskQueue extends Observable {
	private DelayQueue<Task> tasks;
	private Thread thread;
	private String name;
	private WebGate webGate;

	public TaskQueue(String name, WebGate webGate) {
		this.name = name;
		tasks = new DelayQueue<Task>();
		this.webGate = webGate;
		thread = new Thread(() -> {
			try {
				Task task = tasks.take();
				try {
					Object result = task.call();
					TaskQueue.this.setChanged();
					notifyObservers(result);
					System.out.printf("[%s] - Take object = %s%n", Thread.currentThread().getName(), task);
				} catch (Exception ex) {
					// TODO alert Exception when saving
				}
				Thread.sleep(1000);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public void put(Task task) {
		tasks.put(task);
	}

	public void update(Object o, Object id) {
		SaveOperation updateOperation = new SaveOperation(o, id) {
			@Override
			public Object call() throws Exception {
				return webGate.update(o, id);
			}
		};
		put(new Task(updateOperation, 5000));
	}

	public void delete(Object o, Object id) {
		SaveOperation deleteOperation = new SaveOperation(o, id) {
			@Override
			public Object call() throws Exception {
				return webGate.delete(o, id);
			}
		};
		put(new Task(deleteOperation, 10000));
	}

	public void get(Class<? extends Object> clazz, int offset, int limit) {
		SaveOperation getOperation = new SaveOperation() {

			@Override
			public Object call() throws Exception {
				return webGate.getAll(clazz, offset, limit);
			}
		};
		put(new Task(getOperation, 1000));
	}

	public void start() {
		thread.setName(name);
		thread.start();
	}

	public void stop() {
		try {
			while (tasks.poll(10, TimeUnit.SECONDS) != null) {
				// TODO Alert waiting message
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		thread = null;
	}
}

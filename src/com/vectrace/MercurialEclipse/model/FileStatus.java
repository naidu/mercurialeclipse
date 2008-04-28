package com.vectrace.MercurialEclipse.model;

public class FileStatus {

	public static enum Action {
		MODIFIED('M'),
		ADDED('A'),
		REMOVED('R');

		private char action;
		
		private Action(char action) {
			this.action = action;
		}
		
		@Override
		public String toString() {
			return Character.toString(action);
		}
	}

	private Action action;
	private String path;

	public FileStatus(Action action, String path) {
		this.action = action;
		this.path = path;
	}

	public Action getAction() {
		return action;
	}

	public String getPath() {
		return path;
	}
}

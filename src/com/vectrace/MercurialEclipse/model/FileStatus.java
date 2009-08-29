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

	private final Action action;
	private final String path;

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FileStatus [");
        if (action != null) {
            builder.append("action=");
            builder.append(action.name());
            builder.append(", ");
        }
        if (path != null) {
            builder.append("path=");
            builder.append(path);
        }
        builder.append("]");
        return builder.toString();
    }
}

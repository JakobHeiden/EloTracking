package de.neuefische.elotracking.backend.command;

public class MessageContent {

	private String content;

	public MessageContent(String content) {
		this.content = content;
	}

	public MessageContent addNewLine(String newLine) {
		content = content + "\n" + newLine;
		return this;
	}

	public MessageContent makeAllNotBold() {
		content = content.replace("**", "");
		return this;
	}

	public MessageContent makeLastLineBold() {
		String[] lines = content.split("\n");
		lines[lines.length - 1] = "**" + lines[lines.length - 1] + "**";
		content = String.join("\n", lines);
		return this;
	}

	public MessageContent makeAllItalic() {
		content = "*" + content + "*";
		return this;
	}

	public String get() {
		return content;
	}

	public String toString() {
		return get();
	}
}

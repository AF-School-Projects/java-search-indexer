/**
 * 
 * Stores the full path and filename for a corpus document
 * 
 * @author AlexF
 *
 */
class Document {
	String path, name;
	Document(String _path, String _name) { path = _path.trim(); name = _name.trim();}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof Document) {
			Document other = (Document) obj;
			return other.path.equals(this.path);
		}
		return false;
	}
	@Override
	public int hashCode() {
		return path.hashCode();
	}
	@Override
	public String toString() { return name; }
}
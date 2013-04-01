package org.kvj.sierra5.data.provider;

import java.util.List;

import org.kvj.sierra5.common.data.Node;

public interface DataProvider<E extends NodeID> {

	public enum EditType {
		Append, Replace
	};

	public String getId();

	public String getDescription();

	public List<Node<E>> getPath(Node<E> node);

	public Node<E> getRoot();

	public Node<E> find(NodeID id);

	public List<Node<E>> expand(Node<E> node, int type);

	public boolean remove(Node<E> node);

	public String getEditable(Node<E> node);

	public Node<E> edit(EditType type, Node<E> node, String text);

	public boolean upload(Node<E> node, String resource, String location);

	public String download(Node<E> node, String resource);
}

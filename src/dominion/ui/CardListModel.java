package dominion.ui;

import java.util.List;

import javax.swing.AbstractListModel;

public class CardListModel extends AbstractListModel {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("rawtypes")
	private List list;
	
	@SuppressWarnings("rawtypes")
	public CardListModel(List list) {
		this.list = list;
	}

	@Override
	public Object getElementAt(int i) {
		return list.get(i);
	}

	@Override
	public int getSize() {
		return list.size();
	}
	
	public void update() { fireContentsChanged(this, 0, list.size()); }
	@SuppressWarnings("rawtypes")
	public void update(List list) {
		this.list = list;
		fireContentsChanged(this, 0, list.size());
	}
}

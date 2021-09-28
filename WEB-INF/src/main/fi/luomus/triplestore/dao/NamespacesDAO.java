package fi.luomus.triplestore.dao;

import java.util.List;

import fi.luomus.triplestore.models.Namespace;

public interface NamespacesDAO {

	List<Namespace> getNamespaces() throws Exception;

	void upsert(Namespace namespace) throws Exception;

}

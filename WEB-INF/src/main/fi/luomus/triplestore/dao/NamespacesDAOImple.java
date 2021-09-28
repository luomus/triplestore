package fi.luomus.triplestore.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fi.luomus.commons.config.Config;
import fi.luomus.commons.db.connectivity.ConnectionDescription;
import fi.luomus.commons.db.connectivity.SimpleTransactionConnection;
import fi.luomus.commons.db.connectivity.TransactionConnection;
import fi.luomus.triplestore.models.Namespace;

public class NamespacesDAOImple implements NamespacesDAO {

	private final Config config;

	public NamespacesDAOImple(Config config) {
		this.config = config;
	}

	@Override
	public List<Namespace> getNamespaces() throws Exception {
		try (TransactionConnection con = openConnection(); PreparedStatement p = select(con);) {
			try (ResultSet rs = p.executeQuery()) {
				List<Namespace> namespaces = new ArrayList<>();
				while (rs.next()) {
					namespaces.add(createFrom(rs));
				}
				return namespaces;
			}
		}
	}

	private PreparedStatement select(TransactionConnection con) throws SQLException {
		return con.prepareStatement(" SELECT namespace_id, person_in_charge, purpose, namespace_type, qname_prefix FROM namespaces ORDER BY namespace_id");
	}

	private Namespace createFrom(ResultSet rs) throws SQLException {
		return new Namespace(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
	}

	@Override
	public void upsert(Namespace namespace) throws Exception {
		if (getNamespaces().contains(namespace)) {
			update(namespace);
		} else {
			insert(namespace);
		}
	}

	private void insert(Namespace namespace) throws Exception {
		try (TransactionConnection con = openConnection(); PreparedStatement p = insert(con);) {
			setValues(p, namespace);
			p.executeUpdate();
		}
	}

	private void update(Namespace namespace) throws Exception {
		try (TransactionConnection con = openConnection(); PreparedStatement p = update(con);) {
			setValues(p, namespace);
			p.executeUpdate();
		}
	}

	private PreparedStatement insert(TransactionConnection con) throws SQLException {
		return con.prepareStatement(" INSERT INTO namespaces (person_in_charge, purpose, namespace_type, qname_prefix, namespace_id) VALUES (?, ?, ?, ?, ?) ");
	}

	private PreparedStatement update(TransactionConnection con) throws SQLException {
		return con.prepareStatement(" UPDATE namespaces SET person_in_charge = ?, purpose = ?, namespace_type = ?, qname_prefix = ? WHERE namespace_id = ? ");
	}

	private void setValues(PreparedStatement p, Namespace namespace) throws SQLException {
		p.setString(1, namespace.getPersonInCharge());
		p.setString(2, namespace.getPurpose());
		p.setString(3, namespace.getType());
		p.setString(4, namespace.getQnamePrefix());
		p.setString(5, namespace.getNamespace());
	}

	private TransactionConnection openConnection() throws SQLException {
		ConnectionDescription desc = config.connectionDescription("Namespaces");
		return new SimpleTransactionConnection(desc);
	}

}

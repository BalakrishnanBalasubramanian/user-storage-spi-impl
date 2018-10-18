/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.custom.storage.user;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.storage.StorageId;

/**
 * @author <a href="mailto:bbalasub@redhat.com">Bala B</a>
 * @version $Revision: 1 $
 */
@Stateful
public class CustomUserStorageProvider implements ILocalCustomUserStorageProvider {
	private static final Logger logger = Logger.getLogger(CustomUserStorageProvider.class);
	@PersistenceContext
	protected EntityManager em;

	protected ComponentModel model;

	protected KeycloakSession kcSession;

	@Override
	public void setModel(ComponentModel model) {
		this.model = model;
	}

	@Override
	public void setSession(KeycloakSession session) {
		this.kcSession = session;
	}

	@Override
	public void preRemove(RealmModel realm) {

	}

	@Override
	public void preRemove(RealmModel realm, GroupModel group) {

	}

	@Override
	public void preRemove(RealmModel realm, RoleModel role) {

	}

	@Remove
	@Override
	public void close() {
	}

	/**
	 * Get the raw result from the output of the SELECT SQL and put it into an
	 * defined POJO - UserEntity * @param userEntity
	 * 
	 * @return
	 */
	private UserEntity prepareUserEntity(final Object[] userEntity) {
		return new UserEntity(((BigInteger) userEntity[0]).toString(), (String) userEntity[1], (String) userEntity[2],
				(String) userEntity[3]);
	}

	@Override
	public UserModel getUserById(String id, RealmModel realm) {
		logger.info("getUserById: " + id);
		String persistenceId = StorageId.externalId(id);

		Query query = em.createNativeQuery(UserStoreQueries.GET_USER_BY_ID);
		query.setParameter(1, persistenceId);

		Object[] result = (Object[]) query.getSingleResult();

		if (result == null) {
			logger.info("could not find user by id: " + id);
			return null;
		}
		return new UserAdapter(kcSession, realm, model, prepareUserEntity(result));
	}

	@Override
	public UserModel getUserByUsername(String username, RealmModel realm) {
		logger.info("getUserByUsername: " + username);

		Query query = em.createNativeQuery(UserStoreQueries.GET_USER_BY_NAME);
		query.setParameter(1, username);

		List<Object[]> result = query.getResultList();
		if (result == null || result.isEmpty()) {
			logger.info("could not find username: " + username);
			return null;
		}

		return new UserAdapter(kcSession, realm, model, prepareUserEntity(result.get(0)));
	}

	/**
	 * Same functionality but with Stored Procedure
	 * 
	 * @param username
	 * @param realm
	 * @return
	 */
//	@Override
	public UserModel getUserByUsernamebySP(String username, RealmModel realm) {
		logger.info("getUserByUsernameSP: " + username);

		StoredProcedureQuery query = em.createStoredProcedureQuery(UserStoreQueries.GET_USER_BY_NAME_SP);
		query.registerStoredProcedureParameter(UserStoreQueries.GET_USER_BY_NAME_SP_ARG1, String.class,
				ParameterMode.IN);
		query.setParameter(UserStoreQueries.GET_USER_BY_NAME_SP_ARG1, username);
		query.execute();

		List<Object[]> result = query.getResultList();
		if (result == null || result.isEmpty()) {
			logger.info("could not find username: " + username);
			return null;
		}

		return new UserAdapter(kcSession, realm, model, prepareUserEntity(result.get(0)));

	}

	/**
	 * Not implemented.
	 */
	@Override
	public UserModel getUserByEmail(String email, RealmModel realm) {
		return null;
	}

	@Override
	public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
		String password = ((UserAdapter) delegate).getPassword();
		if (password != null) {
			user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
		}
	}

	@Override
	public boolean supportsCredentialType(String credentialType) {
		logger.info("supportsCredentialType: " + CredentialModel.PASSWORD.equals(credentialType));
		return CredentialModel.PASSWORD.equals(credentialType);
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		logger.info("isConfiguredFor: " + (supportsCredentialType(credentialType) && getPassword(user) != null));
		return supportsCredentialType(credentialType) && getPassword(user) != null;
	}

	public String getPassword(UserModel user) {
		String password = null;
		if (user instanceof CachedUserModel) {
			password = (String) ((CachedUserModel) user).getCachedWith().get(PASSWORD_CACHE_KEY);
		} else if (user instanceof UserAdapter) {
			password = ((UserAdapter) user).getPassword();
		}
		return password;
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel))
			return false;
		UserCredentialModel cred = (UserCredentialModel) input;

		Query query = em.createNativeQuery(UserStoreQueries.AUTH_USER_PASS);
		query.setParameter(1, cred.getValue());
		query.setParameter(2, user.getUsername());
		List<Object[]> result = query.getResultList();

		if (result == null || result.isEmpty()) {
			logger.info("could not authenticate the user: " + user.getUsername());
			return false;
		} else {
			return true;
		}
	}

	@Override
	public int getUsersCount(RealmModel realm) {
		Object count = em.createNativeQuery(UserStoreQueries.GET_USER_COUNT).getSingleResult();
		return ((Number) count).intValue();
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm) {
		return getUsers(realm, -1, -1);
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {

		Query query = em.createNativeQuery(UserStoreQueries.GET_ALL_USERS);
		if (firstResult != -1) {
			query.setFirstResult(firstResult);
		}
		if (maxResults != -1) {
			query.setMaxResults(maxResults);
		}
		List<Object[]> results = query.getResultList();
		List<UserModel> users = new LinkedList<>();
		for (Object[] entity : results)
			users.add(new UserAdapter(kcSession, realm, model, prepareUserEntity(entity)));
		return users;
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm) {
		return searchForUser(search, realm, -1, -1);
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
		return searchForUser(search, realm);
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
		return Collections.emptyList();
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult,
			int maxResults) {
		return Collections.emptyList();
	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
		return Collections.emptyList();
	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
		return Collections.emptyList();
	}

	@Override
	public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
		return Collections.emptyList();
	}
}

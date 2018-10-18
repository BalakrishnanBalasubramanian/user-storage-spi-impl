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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

/**
 * @author <a href="mailto:bbalasub@redhat.com">Bala B</a>
 * @version $Revision: 1 $
 */
public class UserAdapter extends AbstractUserAdapterFederatedStorage {
	private static final Logger logger = Logger.getLogger(UserAdapter.class);
	protected UserEntity entity;
	protected String keycloakId;

	public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity entity) {
		super(session, realm, model);
		this.entity = entity;
		keycloakId = StorageId.keycloakId(model, entity.getId());
	}

	public String getPassword() {
		logger.info("In UserAdapter..Getting password from UserEntity - " + entity.getPassword());
		return entity.getPassword();
	}

	public void setPassword(String password) {
		logger.info("In UserAdapter..Setting password to UserEntity");
		entity.setPassword(password);
	}

	@Override
	public String getUsername() {
		logger.info("In UserAdapter..Getting username from UserEntity - " + entity.getUsername());
		return entity.getUsername();
	}

	@Override
	public void setUsername(String username) {
		logger.info("In UserAdapter..Setting username to UserEntity");
		entity.setUsername(username);

	}

	@Override
	public void setEmail(String email) {
		logger.info("In UserAdapter..Setting email to UserEntity");
		entity.setEmail(email);
	}

	@Override
	public String getEmail() {
		logger.info("In UserAdapter..Getting email from UserEntity - " + entity.getEmail());
		return entity.getEmail();
	}

	@Override
	public String getId() {
		return keycloakId;
	}

	@Override
	public void setSingleAttribute(String name, String value) {
		if (name.equals("phone")) {
			entity.setPhone(value);
		} else {
			super.setSingleAttribute(name, value);
		}
	}

	@Override
	public void removeAttribute(String name) {
		if (name.equals("phone")) {
			entity.setPhone(null);
		} else {
			super.removeAttribute(name);
		}
	}

	@Override
	public void setAttribute(String name, List<String> values) {
		if (name.equals("phone")) {
			entity.setPhone(values.get(0));
		} else {
			super.setAttribute(name, values);
		}
	}

	@Override
	public String getFirstAttribute(String name) {
		if (name.equals("phone")) {
			return entity.getPhone();
		} else {
			return super.getFirstAttribute(name);
		}
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		Map<String, List<String>> attrs = super.getAttributes();
		MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
		all.putAll(attrs);
		all.add("phone", entity.getPhone());
		return all;
	}

	@Override
	public List<String> getAttribute(String name) {
		if (name.equals("phone")) {
			List<String> phone = new LinkedList<>();
			phone.add(entity.getPhone());
			return phone;
		} else {
			return super.getAttribute(name);
		}
	}
}

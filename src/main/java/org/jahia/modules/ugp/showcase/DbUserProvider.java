/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.ugp.showcase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.jahia.modules.external.users.BaseUserGroupProvider;
import org.jahia.modules.external.users.UserNotFoundException;
import org.jahia.modules.ugp.showcase.persistence.User;
import org.jahia.modules.ugp.showcase.persistence.UserProperty;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserImpl;
import org.jahia.utils.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database user provider.
 *
 * @author Sergiy Shyrkov
 */
public abstract class DbUserProvider extends BaseUserGroupProvider {

    private static Logger logger = LoggerFactory.getLogger(DbUserProvider.class);

    protected SessionFactory sessionFactoryBean;

    private Criteria addPropertyCriteria(String propName, Criteria query, Properties searchCriteria,
            Criteria propsQuery, List<Criterion> propsFilters) {
        String v = StringUtils.defaultIfBlank(searchCriteria.getProperty(propName), searchCriteria.getProperty("*"));
        if (StringUtils.isNotEmpty(v) && !"*".equals(v)) {
            if (propsQuery == null) {
                propsQuery = query.createCriteria("properties");
            }
            propsFilters.add(Restrictions.and(Restrictions.eq("name", propName),
                    Restrictions.ilike("value", StringUtils.replace(v, "*", "%"))));
        }
        return propsQuery;
    }

    private Criteria filterQuery(Criteria query, Properties searchCriteria) {
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return query;
        }

        String v = StringUtils.defaultIfBlank(searchCriteria.getProperty("username"), searchCriteria.getProperty("*"));
        if (StringUtils.isNotEmpty(v) && !"*".equals(v)) {
            query.add(Restrictions.ilike("username", StringUtils.replace(v, "*", "%")));
        }

        List<Criterion> propsFilters = new LinkedList<>();
        Criteria propsQuery = addPropertyCriteria("j:firstName", query, searchCriteria, null, propsFilters);
        propsQuery = addPropertyCriteria("j:firstName", query, searchCriteria, propsQuery, propsFilters);
        propsQuery = addPropertyCriteria("j:lastName", query, searchCriteria, propsQuery, propsFilters);
        propsQuery = addPropertyCriteria("j:email", query, searchCriteria, propsQuery, propsFilters);

        if (propsQuery != null && !propsFilters.isEmpty()) {
            propsQuery.add(Restrictions.or(propsFilters.toArray(new Criterion[] {})));
        }

        return propsQuery != null ? propsQuery : query;
    }

    @Override
    public JahiaUser getUser(String name) throws UserNotFoundException {
        JahiaUser foundUser = null;
        StatelessSession hib = sessionFactoryBean.openStatelessSession();
        hib.beginTransaction();

        try {
            @SuppressWarnings("unchecked")
            Set<User> users = new LinkedHashSet<>(hib.createCriteria(User.class).add(Restrictions.idEq(name))
                    .setFetchMode("properties", FetchMode.JOIN).list());

            foundUser = !users.isEmpty() ? toUser(users.iterator().next()) : null;
            hib.getTransaction().commit();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
            hib.getTransaction().rollback();
        } finally {
            hib.close();
        }

        if (foundUser == null) {
            throw new UserNotFoundException("Unable to find user " + name + " for the provider " + getKey());
        }

        return foundUser;
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        return true;
    }

    @Override
    public List<String> searchUsers(Properties searchCriteria, long offset, long limit) {

        if (hasUnknownCriteria(searchCriteria, Arrays.asList(new String[] {"username", "j:firstName", "j:lastName", "j:email"}))) {
            return Collections.emptyList();
        }

        List<String> users = null;
        StatelessSession hib = sessionFactoryBean.openStatelessSession();
        hib.beginTransaction();

        try {
            Criteria query = hib.createCriteria(User.class);
            if (offset > 0) {
                query.setFirstResult((int) offset);
            }
            if (limit > 0) {
                query.setMaxResults((int) limit);
            }

            Criteria rootQuery = query;

            query = filterQuery(query, searchCriteria);

            logger.info("Executing query {}", rootQuery);

            @SuppressWarnings("unchecked")
            List<User> list = query.list();
            if (list.size() > 0) {
                users = new LinkedList<>();
                for (User u : list) {
                    users.add(u.getUsername());
                }
            }

            hib.getTransaction().commit();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
            hib.getTransaction().rollback();
        } finally {
            hib.close();
        }

        logger.info("Found {} users matching the search criteria {}", users != null ? users.size() : 0, searchCriteria);

        return users != null ? users : Collections.<String> emptyList();
    }

    public void setSessionFactoryBean(SessionFactory sessionFactoryBean) {
        this.sessionFactoryBean = sessionFactoryBean;
    }

    private JahiaUser toUser(User u) {
        JahiaUserImpl jahiaUser = new JahiaUserImpl(u.getUsername(), u.getUsername(), new Properties(), getKey());
        if (u.getProperties() != null) {
            for (UserProperty up : u.getProperties()) {
                jahiaUser.getProperties().put(up.getName(), up.getValue());
            }
        }
        return jahiaUser;
    }

    @Override
    public boolean verifyPassword(String userName, String userPassword) {
        try {
            JahiaUser u = getUser(userName);

            String pwd = u.getProperty("j:password");
            if (pwd == null && userPassword == null) {
                return true;
            }
            return pwd.equals(EncryptionUtils.sha1DigestLegacy(userPassword));
        } catch (UserNotFoundException e) {
            // ignore
        }
        return false;
    }

    protected static boolean hasUnknownCriteria(Properties searchCriterias, Collection<String> knownColumns) {
        for (Map.Entry<?, ?> entry : searchCriterias.entrySet()) {
            Object name = entry.getKey();
            Object value = entry.getValue();
            if (!(knownColumns.contains(name) || name.equals("*") || value.equals("") || value.equals("*"))) {
                return true;
            }
        }
        return false;
    }
}

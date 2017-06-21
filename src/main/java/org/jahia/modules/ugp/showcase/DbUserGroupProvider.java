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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Restrictions;
import org.jahia.modules.external.users.GroupNotFoundException;
import org.jahia.modules.external.users.Member;
import org.jahia.modules.external.users.Member.MemberType;
import org.jahia.modules.ugp.showcase.persistence.Group;
import org.jahia.modules.ugp.showcase.persistence.GroupMember;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaGroupImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database user / group provider.
 *
 * @author Sergiy Shyrkov
 */
public class DbUserGroupProvider extends DbUserProvider {

    private static Logger logger = LoggerFactory.getLogger(DbUserGroupProvider.class);

    private void filterQuery(Criteria query, Properties searchCriteria) {
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return;
        }

        String v = StringUtils.defaultIfBlank(searchCriteria.getProperty("groupname"), searchCriteria.getProperty("*"));
        if (StringUtils.isNotEmpty(v) && !"*".equals(v)) {
            query.add(Restrictions.ilike("groupname", StringUtils.replace(v, "*", "%")));
        }
    }

    @Override
    public JahiaGroup getGroup(String name) throws GroupNotFoundException {
        JahiaGroup foundGroup = null;
        StatelessSession hib = sessionFactoryBean.openStatelessSession();
        hib.beginTransaction();

        try {
            @SuppressWarnings("unchecked")
            List<Group> groups = hib.createCriteria(Group.class).add(Restrictions.idEq(name)).list();

            foundGroup = !groups.isEmpty() ? toGroup(groups.iterator().next()) : null;
            hib.getTransaction().commit();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
            hib.getTransaction().rollback();
        } finally {
            hib.close();
        }

        if (foundGroup == null) {
            throw new GroupNotFoundException("Unable to find group " + name + " for the provider " + getKey());
        }

        return foundGroup;
    }

    @Override
    public List<Member> getGroupMembers(String groupName) {
        List<Member> members = null;
        StatelessSession hib = sessionFactoryBean.openStatelessSession();
        hib.beginTransaction();

        try {

            @SuppressWarnings("unchecked")
            Set<Group> groups = new LinkedHashSet<>(hib.createCriteria(Group.class).add(Restrictions.idEq(groupName))
                    .setFetchMode("members", FetchMode.JOIN).list());
            if (groups.size() > 0) {
                members = new LinkedList<>();
                for (GroupMember m : groups.iterator().next().getMembers()) {
                    members.add(new Member(m.getName(), MemberType.USER));
                }
            }
            hib.getTransaction().commit();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
            hib.getTransaction().rollback();
        } finally {
            hib.close();
        }

        return members != null ? members : Collections.<Member> emptyList();
    }

    @Override
    public List<String> getMembership(Member member) {
        List<String> owners = null;
        StatelessSession hib = sessionFactoryBean.openStatelessSession();
        hib.beginTransaction();

        try {

            @SuppressWarnings("unchecked")
            List<Group> groups = hib.createCriteria(Group.class).createCriteria("members")
                    .add(Restrictions.eq("name", member.getName())).list();
            if (groups.size() > 0) {
                owners = new LinkedList<>();
                for (Group g : groups) {
                    owners.add(g.getGroupname());
                }
            }
            hib.getTransaction().commit();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
            hib.getTransaction().rollback();
        } finally {
            hib.close();
        }

        return owners != null ? owners : Collections.<String> emptyList();
    }

    @Override
    public List<String> searchGroups(Properties searchCriteria, long offset, long limit) {

        if (hasUnknownCriteria(searchCriteria, Collections.singleton("groupname"))) {
            return Collections.emptyList();
        }

        List<String> groups = null;
        StatelessSession hib = sessionFactoryBean.openStatelessSession();
        hib.beginTransaction();

        try {
            Criteria query = hib.createCriteria(Group.class);
            if (offset > 0) {
                query.setFirstResult((int) offset);
            }
            if (limit > 0) {
                query.setMaxResults((int) limit);
            }

            filterQuery(query, searchCriteria);

            logger.info("Executing query {}", query);

            @SuppressWarnings("unchecked")
            List<Group> list = query.list();
            if (list.size() > 0) {
                groups = new LinkedList<>();
                for (Group g : list) {
                    groups.add(g.getGroupname());
                }
            }

            hib.getTransaction().commit();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
            hib.getTransaction().rollback();
        } finally {
            hib.close();
        }

        logger.info("Found {} groups matching the search criteria {}", groups != null ? groups.size() : 0,
                searchCriteria);

        return groups != null ? groups : Collections.<String> emptyList();
    }

    @Override
    public boolean supportsGroups() {
        return true;
    }

    private JahiaGroup toGroup(Group g) {
        return new JahiaGroupImpl(g.getGroupname(), g.getGroupname(), null, new Properties());
    }

}

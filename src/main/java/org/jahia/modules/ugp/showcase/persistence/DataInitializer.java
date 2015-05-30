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
package org.jahia.modules.ugp.showcase.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.templates.JahiaModuleAware;
import org.jahia.utils.DatabaseUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.UrlResource;

/**
 * Data initializer bean.
 * 
 * @author Sergiy Shyrkov
 */
public class DataInitializer implements InitializingBean, JahiaModuleAware {

    private static Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private Bundle bundle;

    private SessionFactory sessionFactoryBean;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!checkSchema()) {
            createSchema();
        }
        if (!checkData()) {
            populateData();
        }
    }

    private boolean checkData() {
        boolean dataPresent = false;
        StatelessSession hib = sessionFactoryBean.openStatelessSession();
        hib.beginTransaction();

        try {
            long count = ((Number) hib.createCriteria(User.class).setProjection(Projections.rowCount()).uniqueResult())
                    .longValue();
            dataPresent = count > 0;
            hib.getTransaction().commit();
        } catch (HibernateException e) {
            hib.getTransaction().rollback();
        } finally {
            hib.close();
        }
        return dataPresent;
    }

    private boolean checkSchema() {
        boolean tablesExist = false;
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = DatabaseUtils.getDatasource().getConnection();
            rs = conn.getMetaData().getTables(null, null, "SHOWCASE_USER", null);
            tablesExist = rs.next();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return tablesExist;
    }

    private void createSchema() {
        logger.info("Creating showcase DB schema...");

        try {
            String schemaDdl = IOUtils.toString(bundle.getEntry("/META-INF/db/" + DatabaseUtils.getDatabaseType()
                    + "/jahia-usergroup-db-provider-schema.sql"));
            logger.info("Will use schema DDL:\n{}", schemaDdl);
            DatabaseUtils.executeScript(new StringReader(schemaDdl));
            
            logger.info("...showcase DB schema created successfully");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    private String generateUsername(String[] tokens) {
        return new StringBuilder().append(tokens[0]).append(".").append(tokens[1]).toString().toLowerCase();
    }

    private void populateData() throws IOException {
        logger.info("Populating showcase DB schema data...");
        List<String> data = readInitialData();
        Session hibSession = sessionFactoryBean.openSession();
        try {
            hibSession.beginTransaction();
            for (String line : data) {
                User user = toUser(line);
                if (user != null) {
                    hibSession.save(user);
                }
            }
            hibSession.getTransaction().commit();

            logger.info("...done populating showcase DB schema data");
        } catch (RuntimeException e) {
            hibSession.getTransaction().rollback();
            throw e;
        } finally {
            hibSession.close();
        }
    }

    private List<String> readInitialData() throws IOException {
        UrlResource dataResource = new UrlResource(bundle.getEntry("/META-INF/users.csv"));
        try (InputStream is = dataResource.getInputStream()) {
            return IOUtils.readLines(is);
        }
    }

    @Override
    public void setJahiaModule(JahiaTemplatesPackage module) {
        bundle = module.getBundle();
    }

    public void setSessionFactoryBean(SessionFactory sessionFactoryBean) {
        this.sessionFactoryBean = sessionFactoryBean;
    }

    private User toUser(String line) {
        String[] tokens = StringUtils.split(line, " ,");
        User u = new User(generateUsername(tokens));
        u.setProperty("j:firstName", tokens[0]);
        u.setProperty("j:lastName", tokens[1]);
        u.setProperty("j:email", tokens[2]);
        u.setProperty("j:password", "W6ph5Mm5Pz8GgiULbPgzG37mj9g=");

        return u;
    }

}

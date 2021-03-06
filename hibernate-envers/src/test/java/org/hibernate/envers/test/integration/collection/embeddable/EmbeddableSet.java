/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.EmbeddableSetEntity;
import org.hibernate.envers.test.entities.components.Component3;
import org.hibernate.envers.test.entities.components.Component4;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@TestForIssue( jiraKey = "HHH-6613" )
public class EmbeddableSet extends BaseEnversJPAFunctionalTestCase {
	private Integer ese1_id = null;

	private final Component4 c4_1 = new Component4( "c41", "c41_value", "c41_description" );
	private final Component4 c4_2 = new Component4( "c42", "c42_value2", "c42_description" );
	private final Component3 c3_1 = new Component3( "c31", c4_1, c4_2 );
	private final Component3 c3_2 = new Component3( "c32", c4_1, c4_2 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EmbeddableSetEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		EmbeddableSetEntity ese1 = new EmbeddableSetEntity();

		// Revision 1 (ese1: initially 1 element in both collections)
		em.getTransaction().begin();
		ese1.getComponentSet().add( c3_1 );
		em.persist( ese1 );
		em.getTransaction().commit();

		// Revision (still 1) (ese1: removing non-existing element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().remove( c3_2 );
		em.getTransaction().commit();

		// Revision 2 (ese1: adding one element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().add( c3_2 );
		em.getTransaction().commit();

		// Revision 3 (ese1: adding one existing element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().add( c3_1 );
		em.getTransaction().commit();

		// Revision 4 (ese1: removing one existing element)
		em.getTransaction().begin();
		ese1 = em.find( EmbeddableSetEntity.class, ese1.getId() );
		ese1.getComponentSet().remove( c3_2 );
		em.getTransaction().commit();

		ese1_id = ese1.getId();

		em.close();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( EmbeddableSetEntity.class, ese1_id ) );
	}

	@Test
	public void testHistoryOfEse1() {
		EmbeddableSetEntity rev1 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 1 );
		EmbeddableSetEntity rev2 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 2 );
		EmbeddableSetEntity rev3 = getAuditReader().find( EmbeddableSetEntity.class, ese1_id, 3 );

		assertEquals( Collections.singleton( c3_1 ), rev1.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_1, c3_2 ), rev2.getComponentSet() );
		assertEquals( TestTools.makeSet( c3_1 ), rev3.getComponentSet() );
	}
}
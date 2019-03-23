package ru.gisw.caduceus.common.utils;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import java.lang.reflect.*;
import java.util.*;

/**
 * Chigadziko is utility class for create all stateless ejb properties in some object instance
 *
 * It is purposed for implementing "true unit" tests of ejb methods
 * developed for wildfly
 *
 */
public class Chigadziko {

    private static void makePersistenceContext(Map<String,EntityManager> ems, Object... lo)
        throws IllegalAccessException
    {
        Set<Class> cls = new HashSet<>();
        Map<String, EntityManagerFactory> emf = new HashMap<>();
        for (Object o:lo) {
            for (Field f : o.getClass().getDeclaredFields()) {
                PersistenceContext co = (PersistenceContext) f.getAnnotation(PersistenceContext.class);
                if (co != null) {
                    if (co.unitName() != null) {
                        if (!ems.containsKey(co.unitName())) {
                            if (!emf.containsKey(co.unitName())) {
                                emf.put(co.unitName(), Persistence.createEntityManagerFactory(co.unitName()));
                            }
                            ems.put(co.unitName(), emf.get(co.unitName()).createEntityManager());
                        }
                        f.setAccessible(true);
                        f.set(o, ems.get(co.unitName()));
                    }
                }
            }
        }
    }

    /**
     * Set up all @PersistenceContext properties of specified object list (faster than makeEjbContext)
     * @param lo
     * @throws IllegalAccessException
     */
    public static void makePersistenceContext(Object... lo)
        throws IllegalAccessException
    {
        Map<String, EntityManager> co = new HashMap<>();
        makePersistenceContext(co, lo);
    }

    /**
     * Set up all @PersistenceContext properties AND @EJB properties of specified object list (slower than makePersistenceContext)
     * @param lo list of objects for context creation
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */

    public static void makeEjbContext(Object... lo)
        throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException
    {
        Map<String,EntityManager> ems = new HashMap<>();
        for (Object o:lo) {
            for (Field f : o.getClass().getDeclaredFields()) {
                EJB co = (EJB) f.getAnnotation(EJB.class);
                if (co != null) {
                    f.setAccessible(true);
                    if (f.get(o) == null) {
                        Class cla = f.getType();
                        Object fo = cla.getConstructor().newInstance();
                        f.set(o,fo);
                        makePersistenceContext(ems,f.get(o));
                    }
                }
            }
        }
    }


    /**
     * Close all persistence context object for specied onkects
     * @param lo list of objects for context closing
     */
    public static void shutdownEjbContext(Object... lo)
        throws  IllegalAccessException
    {
        Set<EntityManager> ems = new HashSet<>();
        shutdownEjbContext(ems, lo);
        Set<EntityManagerFactory> efs = new HashSet<>();
        for (EntityManager em:ems) {
            efs.add(em.getEntityManagerFactory());
            if (em.isJoinedToTransaction()) {
                em.flush();
            }
            em.clear();
            em.close();
        }
        for (EntityManagerFactory ef:efs) {
            ef.close();
        }
    }

    private static void shutdownEjbContext(Set<EntityManager> ems, Object... lo)
        throws  IllegalAccessException
    {
        if (lo!=null) {
            for (Object o : lo) {
                if (o != null) {
                    if (o.getClass().getDeclaredFields() != null) {
                        for (Field f : o.getClass().getDeclaredFields()) {
                            PersistenceContext co = (PersistenceContext) f.getAnnotation(PersistenceContext.class);
                            if (co != null) {
                                f.setAccessible(true);
                                if (f.get(o) != null) {
                                    ems.add((EntityManager) f.get(o));
                                }
                            }
                        }
                    }
                }
            }
            for (Object o : lo) {
                if (o != null) {
                    if (o.getClass().getDeclaredFields()!=null) {
                        for (Field f : o.getClass().getDeclaredFields()) {
                            EJB co = (EJB) f.getAnnotation(EJB.class);
                            if (co != null) {
                                f.setAccessible(true);
                                shutdownEjbContext(ems, f.get(o));
                            }
                        }
                    }
                }
            }
        }
    }

}

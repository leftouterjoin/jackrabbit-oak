package forstudy;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Test;

public class MBeanUtils {
	@Test
	public void test_showAllMBeanDetail() throws Throwable {
		showAllMBeanDetail(getMBeanServer(), null);
	}

	public static MBeanServerConnection getMBeanServerConnection() throws Throwable {
		/*
		 * -Dcom.sun.management.jmxremote.port=8999
		 * -Dcom.sun.management.jmxremote.ssl=false
		 * -Dcom.sun.management.jmxremote.authenticate=false
		 */
		String hostname = "localhost:8999";
		String serviceUrl = String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", hostname);
		JMXConnector conn = JMXConnectorFactory.connect(new JMXServiceURL(serviceUrl));
		return conn.getMBeanServerConnection();
//		Object o = conn.getMBeanServerConnection().getAttribute(new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
//		CompositeData data = (CompositeData) o;
//		Long used = (Long) data.get("used");
//		System.out.println(used/1024);
	}

	public static MBeanServer getMBeanServer() {
		return ManagementFactory.getPlatformMBeanServer();
	}

	public static void showAllMBeanDetail(MBeanServerConnection mbsc, QueryExp query) throws Throwable {
		Set<ObjectInstance> instances = mbsc.queryMBeans(null, query);
		Iterator<ObjectInstance> iterator = instances.iterator();
		while (iterator.hasNext()) {
			ObjectInstance instance = iterator.next();
			System.out.println("MBean Found:");
			System.out.println("Class Name:\t" + instance.getClassName());
			System.out.println("Object Name:\t" + instance.getObjectName());
			showMBeanInfo(mbsc.getMBeanInfo(instance.getObjectName()));
			System.out.println("****************************************");
		}
	}

    public static void showMBeanInfo(MBeanInfo info) throws Throwable {
        String description = info.getDescription();
        System.out.println("Description: " + description);

        showMBeanAttributeInfo(info);
        showMBeanConstructorInfo(info);
        showMBeanOperationInfo(info);
        showMBeanNotificationInfo(info);
    }

    public static void showMBeanAttributeInfo(MBeanInfo info) {
        MBeanAttributeInfo[] attributes = info.getAttributes();

        if (attributes.length > 0) {
            System.out.println("Attributes:");

            for (MBeanAttributeInfo attribute: attributes) {
                System.out.println("    Name: " + attribute.getName());
                System.out.println("    Description: " + attribute.getDescription());
                System.out.println("    Type: " + attribute.getType());
                if (attribute.isReadable()) {
                    if (attribute.isWritable()) {
                        System.out.println("    Access: RW");
                    } else {
                        System.out.println("    Access: RO");
                    }
                } else {
                    if (attribute.isWritable()) {
                        System.out.println("    Access: WO");
                    }
                }
                System.out.println();
            }
        }
    }

    public  static void showMBeanConstructorInfo(MBeanInfo info) {
        MBeanConstructorInfo[] constructors = info.getConstructors();

        if (constructors.length > 0) {
            System.out.println("Constructors:");

            for (MBeanConstructorInfo constructor: constructors) {
                System.out.println("    Name: " + constructor.getName());
                System.out.println("    Description: " + constructor.getDescription());
                System.out.println("    Signature: ");
                MBeanParameterInfo[] arguments = constructor.getSignature();
                for (MBeanParameterInfo argument: arguments) {
                    System.out.println("        Name: " + argument.getName()
                                       + " Type: " + argument.getType());
                }
                System.out.println();
            }
        }
    }

    public static void showMBeanOperationInfo(MBeanInfo info) {
        MBeanOperationInfo[] operations = info.getOperations();

        if (operations.length > 0) {
            System.out.println("Operations:");

            for (MBeanOperationInfo operation: operations) {
                System.out.println("    Name: " + operation.getName());
                System.out.println("    Description: " + operation.getDescription());
                System.out.println("    Signature: ");
                MBeanParameterInfo[] arguments = operation.getSignature();
                for (MBeanParameterInfo argument: arguments) {
                    System.out.println("        Name: " + argument.getName()
                                       + " Type: " + argument.getType());
                }
                System.out.println("    Return Type: " + operation.getReturnType());
                System.out.println();
            }
        }
    }

    public static void showMBeanNotificationInfo(MBeanInfo info) {
        MBeanNotificationInfo[] notifications = info.getNotifications();

        if (notifications.length > 0) {
            System.out.println("Notifications:");

            for (MBeanNotificationInfo notification: notifications) {
                System.out.println("    Name: " + notification.getName());
                System.out.println("    Description: " + notification.getDescription());
                System.out.println("    NotifType: ");
                String[] types = notification.getNotifTypes();
                for (String type: types) {
                    System.out.println("        Type: " + type);
                }
                System.out.println();
            }
        }
    }
}

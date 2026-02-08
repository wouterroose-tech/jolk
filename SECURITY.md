# **Jolk Security Policy**

#### **1. Experimental Status and Risk Assumption**

The Jolk programming language and the associated Tolk grammar are currently in an **Experimental Phase**.

> **Statement of Risk:** There are no independent security guarantees provided for the Jolk compiler, runtime logic, or Mojo dispatch mechanisms. Users of the Jolk system assume all risks associated with the execution of code, memory safety (within the bounds of the JVM), and logical vulnerabilities.

#### **2. Inherited Security Infrastructure**

Jolk relies strictly on the underlying security architecture of the **Java Virtual Machine (JVM)** and its standard libraries.

* **JVM Sovereignty:** Security is maintained by adhering to the latest security upgrades and patches provided by the JVM distribution (e.g., OpenJDK).
* **Library Dependency:** Any security vulnerabilities identified in third-party Java libraries used by the Jolk compiler are addressed through the standard dependency update cycles of those libraries.

#### **3. Reporting Vulnerabilities**

While the system is provided "as is," the reporting of critical flaws is encouraged to improve the **Nominalised Precision** of the engine.

* **Procedure:** Please report any identified security flaws via GitHub Private vulnerability reporting.
* **Public Disclosure:** As there is no dedicated security team, we request a coordinated disclosure approach to allow the maintainer time to evaluate the impact on the **Tolk Grammar** architecture.

#### **4. Security Upgrades**

Users are responsible for ensuring their local environment is running a supported and patched version of the Java Development Kit (JDK). The Jolk project does not provide independent security backports for older versions of the JVM.

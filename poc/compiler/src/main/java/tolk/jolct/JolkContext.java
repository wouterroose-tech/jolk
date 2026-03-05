package tolk.jolct;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JolkContext {
    private final Set<String> jolkTypes = new HashSet<>();
    private final Set<String> jolkClasses = new HashSet<>();
    private final Set<String> crtpTypes = new HashSet<>();

    public void addJolkType(String typeName) {
        jolkTypes.add(typeName);
    }

    public void addCrtpType(String typeName) {
        crtpTypes.add(typeName);
    }

    public void addJolkClass(String typeName) {
        jolkTypes.add(typeName);
        jolkClasses.add(typeName);
        crtpTypes.add(typeName);
    }

    public boolean isCrtpType(String typeName, String currentPackage, List<String> imports) {
        return checkSet(crtpTypes, typeName, currentPackage, imports);
    }

    public boolean isJolkType(String typeName, String currentPackage, List<String> imports) {
        return checkSet(jolkTypes, typeName, currentPackage, imports);
    }

    public boolean isJolkClass(String typeName, String currentPackage, List<String> imports) {
        return checkSet(jolkClasses, typeName, currentPackage, imports);
    }

    private boolean checkSet(Set<String> set, String typeName, String currentPackage, List<String> imports) {
        if (set.contains(typeName)) return true;
        if (set.contains(currentPackage + "." + typeName)) return true;
        for (String imp : imports) {
            if (imp.endsWith(".*")) {
                String pkg = imp.substring(0, imp.length() - 2);
                if (set.contains(pkg + "." + typeName)) return true;
            } else if (imp.endsWith("." + typeName)) {
                if (set.contains(imp)) return true;
            }
        }
        return false;
    }
}
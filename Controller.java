// Package/import: adjust to match your project (e.g. "package ch.xxx;" and import the COntrollerInterface)
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

// ...existing code...

// If COntrollerInterface is in a package, add the proper import or place this file in the same package.
// Example: import your.package.COntrollerInterface;

public class Controller implements COntrollerInterface {
    // ...existing code...

    // Thread-safety / atomicity
    private final ReentrantLock lock = new ReentrantLock();

    // Active feature set
    private final Set<String> activeFeatures = new HashSet<>();

    // Observers notified after successful commit
    private final List<FeatureChangeListener> listeners = new ArrayList<>();

    // Simple incompatibility map: feature -> set of incompatible features
    private final Map<String, Set<String>> incompatibilities = new HashMap<>();

    // Pluggable validators: each can inspect the current+proposed state and veto
    private final List<FeatureValidator> validators = new ArrayList<>();

    // --- Public API (matches interface methods in spirit) ---

    // Process a command like:
    // "activate feature1, feature2; deactivate feature3, feature4;"
    public CommandResult processCommand(String command) {
        if (command == null) {
            return CommandResult.error("command is null");
        }

        // Parse into activation/deactivation lists
        ParsedCommand parsed = parse(command);
        if (!parsed.ok) {
            return CommandResult.error(parsed.error);
        }

        // Apply atomically
        lock.lock();
        try {
            Set<String> staged = new HashSet<>(activeFeatures);

            // perform deactivations first (order doesn't matter)
            for (String f : parsed.deactivate) {
                staged.remove(f);
            }

            // then activations
            for (String f : parsed.activate) {
                staged.add(f);
            }

            // Interaction checks: incompatibilities
            for (String f : staged) {
                Set<String> incompatible = incompatibilities.getOrDefault(f, Collections.emptySet());
                for (String other : incompatible) {
                    if (staged.contains(other)) {
                        String msg = String.format("incompatibility: '%s' cannot coexist with '%s'", f, other);
                        return CommandResult.error(msg);
                    }
                }
            }

            // Run validators
            for (FeatureValidator v : validators) {
                Optional<String> veto = v.validate(Collections.unmodifiableSet(activeFeatures), Collections.unmodifiableSet(staged));
                if (veto.isPresent()) {
                    return CommandResult.error("validator veto: " + veto.get());
                }
            }

            // commit
            Set<String> previous = new HashSet<>(activeFeatures);
            activeFeatures.clear();
            activeFeatures.addAll(staged);

            // compute diffs
            Set<String> added = new HashSet<>(activeFeatures);
            added.removeAll(previous);
            Set<String> removed = new HashSet<>(previous);
            removed.removeAll(activeFeatures);

            // notify observers outside of internal state modification but still while lock is held minimal.
            notifyListeners(added, removed);

            return CommandResult.ok(added, removed);
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getActiveFeatures() {
        lock.lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(activeFeatures));
        } finally {
            lock.unlock();
        }
    }

    public void addListener(FeatureChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    public void removeListener(FeatureChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    public void registerIncompatibility(String feature, Collection<String> incompatibleWith) {
        Objects.requireNonNull(feature);
        Objects.requireNonNull(incompatibleWith);
        synchronized (incompatibilities) {
            incompatibilities.computeIfAbsent(feature, k -> new HashSet<>()).addAll(incompatibleWith);
            // also add reverse mapping
            for (String other : incompatibleWith) {
                incompatibilities.computeIfAbsent(other, k -> new HashSet<>()).add(feature);
            }
        }
    }

    public void registerValidator(FeatureValidator validator) {
        Objects.requireNonNull(validator);
        validators.add(validator);
    }

    // --- Parsing logic ---
    private ParsedCommand parse(String command) {
        // normalize separators and split by ';'
        String[] groups = command.split(";");
        Set<String> toActivate = new LinkedHashSet<>();
        Set<String> toDeactivate = new LinkedHashSet<>();
        for (String group : groups) {
            String g = group.trim();
            if (g.isEmpty()) continue;
            String lower = g.toLowerCase(Locale.ROOT);
            if (lower.startsWith("activate")) {
                String rest = g.substring(8).trim(); // after 'activate'
                toActivate.addAll(splitFeatures(rest));
            } else if (lower.startsWith("deactivate")) {
                String rest = g.substring(10).trim(); // after 'deactivate'
                toDeactivate.addAll(splitFeatures(rest));
            } else {
                return ParsedCommand.error("unknown group (expected 'activate' or 'deactivate'): " + g);
            }
        }
        return ParsedCommand.ok(toActivate, toDeactivate);
    }

    private List<String> splitFeatures(String commaList) {
        if (commaList == null || commaList.isEmpty()) return Collections.emptyList();
        return Arrays.stream(commaList.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    // --- Observers ---
    private void notifyListeners(Set<String> added, Set<String> removed) {
        List<FeatureChangeListener> snapshot;
        synchronized (listeners) {
            snapshot = new ArrayList<>(listeners);
        }
        for (FeatureChangeListener l : snapshot) {
            try {
                l.onFeaturesChanged(Collections.unmodifiableSet(added), Collections.unmodifiableSet(removed));
            } catch (Exception ex) {
                // swallow listener exceptions to keep controller consistent
            }
        }
    }

    // --- Helper types ---

    public static final class CommandResult {
        public final boolean success;
        public final String error;
        public final Set<String> added;
        public final Set<String> removed;

        private CommandResult(boolean success, String error, Set<String> added, Set<String> removed) {
            this.success = success;
            this.error = error;
            this.added = added != null ? Collections.unmodifiableSet(added) : Collections.emptySet();
            this.removed = removed != null ? Collections.unmodifiableSet(removed) : Collections.emptySet();
        }

        public static CommandResult ok(Set<String> added, Set<String> removed) {
            return new CommandResult(true, null, added, removed);
        }

        public static CommandResult error(String message) {
            return new CommandResult(false, message, Collections.emptySet(), Collections.emptySet());
        }
    }

    private static final class ParsedCommand {
        final boolean ok;
        final String error;
        final Set<String> activate;
        final Set<String> deactivate;

        private ParsedCommand(boolean ok, String error, Set<String> activate, Set<String> deactivate) {
            this.ok = ok;
            this.error = error;
            this.activate = activate;
            this.deactivate = deactivate;
        }

        static ParsedCommand ok(Set<String> a, Set<String> d) {
            return new ParsedCommand(true, null, a, d);
        }

        static ParsedCommand error(String err) {
            return new ParsedCommand(false, err, Collections.emptySet(), Collections.emptySet());
        }
    }

    // Listener interface for observers
    public interface FeatureChangeListener {
        // called after a successful commit; both sets are immutable snapshots
        void onFeaturesChanged(Set<String> added, Set<String> removed);
    }

    // Validator: return Optional.empty() to accept, or Optional.of(reason) to veto
    public interface FeatureValidator {
        Optional<String> validate(Set<String> current, Set<String> proposed);
    }

    // ...existing code...
}


package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.integration.crafttweaker.CtOreTreeBuilder;
import cn.mcmod.neotreeores.integration.crafttweaker.ZenOreTrees;
import stanhebben.zenscript.IZenErrorLogger;
import stanhebben.zenscript.ZenModule;
import stanhebben.zenscript.impl.GenericCompileEnvironment;
import stanhebben.zenscript.impl.GenericRegistry;
import stanhebben.zenscript.util.ZenPosition;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Compiles and executes the actual user's .zs with the installed ZenScript compiler. */
public final class CtZenRegression implements IZenErrorLogger {
    private int errors;
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("script path required");
        String script = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        if (CtScriptScanner.scanText("example.zs", script) != 3) throw new AssertionError("pre-scan");
        CtTrees.freeze();
        CtZenRegression logger = new CtZenRegression();
        GenericCompileEnvironment environment = new GenericCompileEnvironment();
        GenericRegistry registry = new GenericRegistry(environment, logger);
        environment.setRegistry(registry);
        registry.registerNativeClass(CtOreTreeBuilder.class);
        registry.registerNativeClass(ZenOreTrees.class);
        ZenModule module = ZenModule.compileScriptString(script, "example.zs", environment, CtZenRegression.class.getClassLoader());
        if (logger.errors != 0 || module == null) throw new AssertionError("ZenScript compile errors: " + logger.errors);
        module.getMain().run();
        if (!CtTrees.isRemoved(OreTreeType.DIAMOND) || CtTrees.recipeRequired(OreTreeType.EMERALD) != 1
                || CtTrees.recipeYield(OreTreeType.EMERALD) != 64 || CtTrees.allowsDimension(OreTreeType.EMERALD, -1))
            throw new AssertionError("runtime replay changed final declarations");
        System.out.println("PASS CtZenRegression: user's example.zs compiled and executed; zero ZenScript errors");
    }
    public void error(ZenPosition pos, String message) { error(pos + ": " + message); }
    public void warning(ZenPosition pos, String message) { warning(pos + ": " + message); }
    public void info(ZenPosition pos, String message) { info(pos + ": " + message); }
    public void error(String message) { errors++; System.err.println("ZEN ERROR " + message); }
    public void error(String message, Throwable error) { errors++; System.err.println(message); error.printStackTrace(); }
    public void warning(String message) { System.out.println("ZEN WARNING " + message); }
    public void info(String message) { System.out.println("ZEN INFO " + message); }
}

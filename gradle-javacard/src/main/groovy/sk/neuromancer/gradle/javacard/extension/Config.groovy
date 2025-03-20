/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2017 Bertrand Martel
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package sk.neuromancer.gradle.javacard.extension

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ClosureBackedAction

import javax.inject.Inject

/**
 * JavaCard extension object (the same as defined in https://github.com/martinpaljak/ant-javacard#syntax
 *
 * @author Bertrand Martel
 */
abstract class Config {

    /**
     * path to the JavaCard SDK that is used if individual cap does not specify one. Optional if cap defines one, required otherwise.
     */
    String jckit

    Object jcardSim

    Object gptoolVersion

    /**
     * @deprecated Has no effect, will be removed in future release
     */
    boolean addSurrogateJcardSimRepo = false

    boolean addImplicitJcardSim = true

    boolean addImplicitJcardSimJunit = true

    boolean debugGpPro = false

    boolean fixClassPath = true

    /**
     * log level to set to "VERBOSE","DEBUG","INFO","WARN" or "ERROR", default is "INFO"
     */
    String logLevel = 'INFO'

    /**
     * Classpath for Ant
     */
    String antClassPath = null

    /**
     * list of cap files to build.
     */
    List<Cap> caps = []

    /**
     * list of additional GPPro args for the install task
     */
    List<String> installGpProArgs = []

    Cap cap(Closure closure) {
        cap(ClosureBackedAction.of(closure))
    }

    Cap cap(Action<Cap> action) {
        def someCap = objectFactory.newInstance(Cap)
        action.execute(someCap)
        caps.add(someCap)
        return someCap
    }

    void installGpProArgs(String arg) {
        this.installGpProArgs.add(arg)
    }

    void jckit(String path) {
        this.jckit = path
    }

    void antClassPath(String classPath) {
        this.antClassPath = classPath
    }

    void logLevel(String logLevel) {
        this.logLevel = logLevel
    }

    void jcardSim(Object jcardSim) {
        this.jcardSim = jcardSim
    }

    /**
     * @deprecated Has no effect, will be removed in future release
     */
    void addSurrogateJcardSimRepo(Boolean addIt) {
        this.addSurrogateJcardSimRepo = addIt
    }

    void addImplicitJcardSim(Boolean addIt) {
        this.addImplicitJcardSim = addIt
    }

    void addImplicitJcardSimJunit(Boolean addIt) {
        this.addImplicitJcardSimJunit = addIt
    }

    void debugGpPro(Boolean val) {
        this.debugGpPro = val
    }

    void fixClassPath(Boolean val) {
        this.fixClassPath = val
    }

    /**
     * Validate fields
     */
    def validate(Project project) {
        checkJckit(project)
        checkOutput()
        checkAppletClass()
        checkDependency()
    }

    /**
     * Check that an exp is defined for import object nested in cap.
     */
    def checkDependency() {
        caps.each { capItem ->
            if (capItem.dependencies != null) {
                capItem.dependencies.local.each { importItem ->
                    if (!importItem.exps?.trim() || !importItem.jar?.trim()) {
                        throw new InvalidUserDataException('import exp/jar is required for local dependency')
                    }
                }
            }
        }
    }

    /**
     * Check that a className is defined for each applet nested in cap.
     */
    def checkAppletClass() {
        caps.each { capItem ->
            capItem.applets.each { appletItem ->
                if (!appletItem.className?.trim()) {
                    throw new InvalidUserDataException('applet className is required')
                }
            }
        }
    }

    /**
     * Check that output field is defined.
     */
    def checkOutput() {
        caps.each { capItem ->
            if (!capItem.output?.trim()) {
                throw new InvalidUserDataException('cap output is required')
            }
        }
    }

    /**
     * Check that jckit is defined either in the root javacard object or in all caps object if not in environment variable.
     */
    def checkJckit(Project project) {
        if (jckit?.trim()) {
            def folder = project.file(jckit)
            if (!folder.exists()) {
                throw new InvalidUserDataException('Invalid JavaCard SDK path : ' + folder.getAbsolutePath())
            }
            jckit = folder.getAbsolutePath()
        } else if (caps.size() > 0) {
            caps.each { capItem ->
                if (!capItem.jckit?.trim() && !System.env['JC_HOME']) {
                    throw new InvalidUserDataException('Invalid JavaCard SDK path : use JC_HOME or jckit')
                } else if (capItem.jckit?.trim() && !System.env['JC_HOME']) {
                    def folder = project.file(capItem.jckit)
                    if (!folder.exists()) {
                        throw new InvalidUserDataException('Invalid JavaCard SDK path : ' + folder.getAbsolutePath())
                    }
                    capItem.jckit = folder.getAbsolutePath()
                }
            }
        } else {
            throw new InvalidUserDataException('no caps were referenced')
        }
    }

    /**
     * Get Javacard SDK
     * @return
     */
    String getJcKit() {
        if (System.env['JC_HOME']) {
            return System.env['JC_HOME']
        } else if (jckit?.trim()) {
            return jckit
        } else if (caps.size() > 0) {
            def kit = ""
            caps.each { capItem ->
                if (capItem.jckit?.trim()) {
                    kit = capItem.jckit
                }
            }
            return kit
        }
        return ""
    }

    @Inject
    abstract ObjectFactory getObjectFactory()
}
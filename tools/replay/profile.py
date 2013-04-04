import logging
import platform
import resource
import time

from datetime import datetime

class ExecutionRecorder:
    """
    ExecutionRecorder objects concentrate the tools for collecting profiles of
    execution time and resource usage.
    
    When an ExecutionRecorder is created, it records the current time, memory
    consumption, and cpu utilization. When the recorder is stopped, it again
    records the time, memory and cpu usage. After stopping the recorder, the
    execution time and resource usage data may be dumped for later analysis
    using the @c logging.debug channel. 

    For example, suppose you want to collect resource information of an
    algorithm implementation. The following code measures the used resources
    and writes them to the program's log.
    
    @code
    er = ExecutionRecorder()       # Start recorder
    algo( some_parameter_set )     # Invoke algorithm
    er.stop()
    er.dump()                      # Write the recorded information to the log
    @endcode
    
    @note      It is the user's responsibility to dump() the collected
               information; an ExecutionRecorder will not write information to
               the log on its own. This allows throwing away incomplete
               data sets.
               
    @see The @c resource module.
    """
    
    def __init__(self):
        self.__stop_time = None
        self.__stop_resources = None

        # Use milliseconds for all times to ease further processing
        self.__start_time = time.time()
        self.__start_resources = resource.getrusage(resource.RUSAGE_SELF)


    def is_running(self):
        """
        Check whether the object currently records data.
        
        @return    @c True, if execution data is being recorded at the moment;
                   @c False otherwise.
        """
        # There is no way to restart; stop() is the only method setting
        # this variable.
        return self.__stop_time is None
    

    def stop(self):
        """
        End data acquisition.
        
        @note  There is no way to restart the ExecutionRecorder.
        """
        if not self.is_running():
            return
        
        # Stop resource monitoring.
        self.__stop_time = time.time()
        self.__stop_resources = resource.getrusage(resource.RUSAGE_SELF)


    def dump_data(self, extra_information={}):
        """
        Write the collected information to the program's log using @c logging.debug
        
        Calling this method implies a call of stop(); thus data recording ends.
        
        @param     extra_information   A dictionary with additional entries to
                                       the @c .timing file. Use this, for
                                       example, to record the results of the
                                       profiled execution. 
        """
        if self.is_running():
            self.stop()
        
        # Compute the resource usage
        wall_time = self.__stop_time - self.__start_time
        user_time = self.__stop_resources.ru_utime - self.__start_resources.ru_utime
        sys_time = self.__stop_resources.ru_stime - self.__start_resources.ru_stime
        cpu_time = user_time + sys_time
        max_rss = self.__stop_resources.ru_maxrss
        
        info = [ "node: {0}".format(platform.node()),
                 "platform: {0}".format(platform.platform()),
                 "python: {0}".format(platform.python_version()),
                 "date (Y/M/D h:m:s): {0}".format(
                          datetime.now().strftime("%Y/%m/%d %H:%M:%S")
                      ),
                 "wall time (s): {0}".format(wall_time),
                 "user time (s): {0}".format(user_time),
                 "sys time (s): {0}".format(sys_time),
                 "cpu time (s): {0}".format(cpu_time),
                 "max memory (kB): {0}".format(max_rss),
             ]
        for key, value in sorted(extra_information.items()):
            info.append("{0}: {1}".format(key, value))
            
        # Finally, dump the resource information
        for item in info:
            logging.debug(item)

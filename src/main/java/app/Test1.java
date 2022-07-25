package app;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;

public class Test1 {
	public static void main(String[] args) throws Exception {
		ParameterTool parameterTool = ParameterTool.fromArgs(args);
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
		env.getConfig().setGlobalJobParameters(parameterTool);
		String hostname = parameterTool.get("hostname");
		int port = parameterTool.getInt("port");
		DataStreamSource<String> text = env.socketTextStream(hostname, port, "\n");
		DataStream<WordWithCount> windowCounts = text.flatMap((FlatMapFunction<String, WordWithCount>) (value, out) -> {
			for (String word : value.split("\\s")) {
				out.collect(new WordWithCount(word, 1L));
			}
		}).returns(TypeInformation.of(WordWithCount.class)).keyBy("word")
		.timeWindow(Time.seconds(5))
		.reduce((ReduceFunction<WordWithCount>) (a, b) -> new WordWithCount(a.word, a.count + b.count));
		windowCounts.print().setParallelism(1);
		env.execute("Socket Window WordCount");
		
	}
	
	public static class WordWithCount {
		
		public String word;
		public long count;
		
		public WordWithCount() {
		}
		
		public WordWithCount(String word, long count) {
			this.word = word;
			this.count = count;
		}
		
		@Override
		public String toString() {
			return word + " : " + count;
		}
	}
	
}
package erwins.util.groovy


import java.io.File

import org.apache.commons.collections.map.ListOrderedMap

import erwins.util.collections.MapForList
import erwins.util.collections.MapType
import erwins.util.lib.CollectionUtil
import erwins.util.lib.StringUtil
import erwins.util.lib.security.MD5
import groovy.sql.GroovyRowResult

/** SQLUtil은 싱글톤만 사용하니까 따로 뺐다.  */
public class GroovyMetaUtil{

	public static void addMeta(){
		map()
		file()
		stringArray()
		list()
		groovyRowResult 'N/A'
	}

	/** 이게 더 깔끔한듯 */
	public static void map(){
		HashMap.metaClass."plus" = { key,value=1 ->
			def org = delegate.get(key)
			if(org==null) org = 0;
			delegate.put key, org+value
		}
		ListOrderedMap.metaClass."merge" = { map, keys ->
			keys.each {
				if(!map.containsKey(it)) return
				delegate.put it,map[it] 
			}
			return delegate
		}
		/** 맵의 키값만 변경한다. 자동DB인설트 등에 사용하자. 
		 * keyMap = [원래키 : 바꿀키]*/
		ListOrderedMap.metaClass."mapping" = {Map keyMap ->
			def newMap = new ListOrderedMap()
			keyMap.each {
				newMap[it.value] = delegate[it.key]
			}
			return newMap
		}
		/** 각 키값들을 DB에 들어가는 구조로 바꾼다. 
		 * 도메인 명칭(접미) -> 도메인_명칭 */
		ListOrderedMap.metaClass."toUnderscore" = {
			def newMap = new ListOrderedMap()
			delegate.each {
				def key = StringUtil.getUnderscore(it.key).replaceAll(/\(.*\)/, '').replaceAll(' ','_')
				newMap[key] = delegate[it.key]
			}
			return newMap
		}
		/** 맵의 키값만 변경한다. 자동DB인설트 등에 사용하자.
		* keyMap = [원래키 순서대로,,]*/
		def mapping =  {List keyList ->
			def newMap = new ListOrderedMap()
			keyList.each {
				newMap[it] = delegate[it]
			}
			return newMap
		}
		ListOrderedMap.metaClass."mapping" = mapping
		GroovyRowResult.metaClass."mapping"  = mapping
	}

	public static void file(){
		/** 해당 확장자의 파일을 모두 가져온다 */
		File.metaClass."listAll" = {endsWith = null ->
			def files = [];
			delegate.eachFileRecurse{if(it.file) files << it};
			if(endsWith==null) files
			else files.findAll { it.name.endsWith(endsWith) }
		}
		/**  파일 이름에 관계없이 동일파일을 Hash기준으로 알려준다. */
		File.metaClass."duplicated" = {endsWith = null ->
			MapForList<File> map = new MapForList<File>(MapType.ListOrderd);
			delegate.listAll(endsWith).each { map.add MD5.getHashHexString(it.name), it  }
			return map
		}
	}

	/** 이외 유용한것들
	 * min / max / split / sort / sum / unique
	 * assert [0:[2,4,6], 1:[1,3,5]] == [1,2,3,4,5,6].groupBy { it % 2 }
	 *   */
	public static void list(){
		/** List<Map> 을 Map<List<Map>> 으로 변경 */
		ArrayList.metaClass."toMap" = { key ->
			def map = [:];
			delegate.each { map.put it[key],it  }
			return map
		}
		ArrayList.metaClass."toUnderscore" = { key ->
			delegate.collect { it.toUnderscore() }
		}
		/** List<Map> 을 Map<List<Map>> 으로 변경 */
		ArrayList.metaClass."mapping" = {String key ->
			def mapList = new MapForList(MapType.ListOrderd) //일단 기본정렬순으로
			delegate.each { mapList.add it[key],it  }
			return mapList
		}
		ArrayList.metaClass."mapping" = {List keys ->
			def mapList = new MapForList(MapType.ListOrderd) //일단 기본정렬순으로
			delegate.each {
				def key = keys.collect { k -> it[k]  }.join('|')
				mapList.add key,it
			}
			return mapList
		}
		/** 그냥 split은 true / false 구조로 무조건 2개로 나눈다. 이는 그것을 개량한것이다.
		 * separator이 true로 나올때마다 하나의 리스트를 추가한다.
		 * List<List>의 구조를 가진다. 첫번째 separator는 무조건 true가 나와야 한다. */
		ArrayList.metaClass."splitByFirst" = { separator ->
			def result = []
			def nowList
			delegate.each {
				if(separator(it)){
					nowList = []
					result << nowList
				}
				nowList << it
			}
			return result
		}
		/** List<Map> 인 구조에서 특정 key로 데이터를 분류한다.
		 * 분류된 데이터는 당근 Map<List<Map>> 이 된다  */
		ArrayList.metaClass."splitByKey" = {key ->
			def map = new MapForList()
			delegate.each { map.add it[key], it}
			return map
		}
		/** List<Map> 인 구조에서 특정 key로 데이터를 정렬한다 */
		ArrayList.metaClass."sortKey" = {String key ->
			delegate.sort {a,b -> a[key].compareTo(b[key])}
			return delegate
		}
		/** List<Map> 인 구조에서 특정 key로 데이터를 정렬한다 */
		ArrayList.metaClass."sortKey" = {List keys ->
			def comparator = CollectionUtil.mapComparator(keys)
			Collections.sort(delegate,comparator);
			return delegate
		}
		/** List<Map> 인 구조에서 현제 데이터와 이전 데이터를 비교하고싶을때 사용한다.
		 * 새로운 List를 리턴한다 */
		ArrayList.metaClass."interval" = { key , beforeKey ->
			def before
			delegate.each {
				if(before!=null) it[beforeKey] = before[key]
				before = it
			}
			delegate.remove 0
			return delegate
		}
	}

	/**
	 * List<Map> 데이터들의 특정값 중복 체크. 
	 * 이후 map.findAll { it.value==1 } 등을 사용하자. -> 테스트 필요
	 *  keyList로 :를 사용해 연결하자. */
	public static Map duplicated(list,key){
		def map = [:]
		list.each{ map.plus it[key]}
		return map
	}

	/** 미칠듯이 편하다. 이거 굿! */
	public static void groovyRowResult(nullString){
		GroovyRowResult.metaClass."getNullSafe"  = { key ->
			if(delegate.containsKey(key)) return delegate[key]
			else return nullString
		}
	}
	/** 추가하진 않는다. 나중에 하든지 하자. */
	public static void string(){
		String.metaClass.swapCase = {
			def sb = new StringBuffer()
			delegate.each {
				sb << (Character.isUpperCase(it as char) ? Character.toLowerCase(it as char) :
						Character.toUpperCase(it as char))
			}
			sb.toString()
		}
	}

	public static void stringArray(){
		String[].metaClass."insertSql"  = {tableName ->
			def parameter = StringUtil.iterateStr( '?', ',', delegate.length)
			def INSERT = "INSERT INTO $tableName ( ${delegate.join(',')} ) VALUES ( ${parameter} )"
			return INSERT
		}
	}
}

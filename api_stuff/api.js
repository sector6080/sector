import {createClient} from '@supabase/supabase-js';

const supabaseUrl = 'https://jnfqfowidqwrmmvxnvhe.supabase.co'; // API URL
const supabaseKey = 'sb_publishable_Lbh2m9zIjUCwEF6PVNbc4g_XkdJ7Gcz'; // API key

const supabase = createClient(supabaseUrl, supabaseKey);

export default supabase;